"""Genera la lámina de cobertura US3.1/US3.2 desde JaCoCo y Surefire."""
from pathlib import Path
import json
import subprocess
import tempfile
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[3]
JACOCO = ROOT / "backend" / "target" / "site" / "jacoco" / "jacoco.xml"
SUREFIRE = ROOT / "backend" / "target" / "surefire-reports"
OUTPUT = Path(__file__).resolve().parents[1] / "evidencias" / "cobertura" / "US31-US32-jacoco-resumen.png"
RENDERER = Path(__file__).with_name("renderizar_evidencia_cobertura.ps1")

TARGETS = [
    ("US3.1", "TimeTracking", "from"),
    ("US3.1", "TimeTracking", "totalSecondsAt"),
    ("US3.1", "TimeTracking", "toMinutes"),
    ("US3.1", "Task", "transition"),
    ("US3.1", "Task", "refreshActualDuration"),
    ("US3.1", "Task", "timeTracking"),
    ("US3.1", "Subtask", "transition"),
    ("US3.2", "TaskCommandService", "transition"),
    ("US3.2", "SubtaskCommandService", "execute"),
    ("US3.2", "SubtaskCommandService", "load"),
    ("US3.2", "TaskQueryService", "listAssignedTasks"),
    ("US3.2", "TaskQueryService", "toAssigned"),
    ("US3.2", "TaskController", "assignedTasks"),
    ("US3.2", "TaskController", "start"),
]
EXPECTED_TESTS = 138
EXPECTED_GLOBAL_LINE_PERCENT = 70.06


def counter(element, kind):
    found = next((item for item in element.findall("counter") if item.get("type") == kind), None)
    return (0, 0) if found is None else (int(found.get("covered")), int(found.get("missed")))


def add_pair(total, pair):
    total[0] += pair[0]
    total[1] += pair[1]


def class_index(report):
    return {
        element.get("name").split("/")[-1]: element
        for element in report.findall("./package/class")
    }


def main():
    if not JACOCO.exists():
        raise SystemExit("Falta jacoco.xml; ejecuta mvn clean verify desde backend")
    if not SUREFIRE.is_dir():
        raise SystemExit("Falta backend/target/surefire-reports; ejecuta mvn clean verify")

    report = ET.parse(JACOCO).getroot()
    classes = class_index(report)
    rows = []
    story_lines = {"US3.1": [0, 0], "US3.2": [0, 0]}
    story_branches = {"US3.1": [0, 0], "US3.2": [0, 0]}
    for story, class_name, method_name in TARGETS:
        if class_name not in classes:
            raise SystemExit(f"No se encontró la clase JaCoCo {class_name}")
        methods = [method for method in classes[class_name].findall("method")
                   if method.get("name") == method_name]
        if len(methods) != 1:
            raise SystemExit(
                f"Método {class_name}.{method_name}: se esperaba 1 coincidencia, hay {len(methods)}"
            )
        lines = counter(methods[0], "LINE")
        branches = counter(methods[0], "BRANCH")
        add_pair(story_lines[story], lines)
        add_pair(story_branches[story], branches)
        branch_total = sum(branches)
        fully_covered = lines[1] == 0 and (branch_total == 0 or branches[1] == 0)
        rows.append({
            "story": story,
            "method": f"{class_name}.{method_name}",
            "line_covered": lines[0],
            "line_missed": lines[1],
            "branch_covered": branches[0],
            "branch_missed": branches[1],
            "result": "CUBIERTO" if fully_covered else "PARCIAL",
        })

    total_tests = failures = errors = skipped = 0
    for path in SUREFIRE.glob("TEST-*.xml"):
        suite = ET.parse(path).getroot()
        total_tests += int(suite.get("tests", 0))
        failures += int(suite.get("failures", 0))
        errors += int(suite.get("errors", 0))
        skipped += int(suite.get("skipped", 0))

    overall_lines = counter(report, "LINE")
    global_line_percent = 100.0 * overall_lines[0] / sum(overall_lines)
    selected_lines = [sum(story_lines[story][i] for story in story_lines) for i in range(2)]
    selected_branches = [sum(story_branches[story][i] for story in story_branches) for i in range(2)]
    if total_tests != EXPECTED_TESTS:
        raise SystemExit(f"Se esperaban {EXPECTED_TESTS} pruebas backend; JaCoCo/Surefire informa {total_tests}")
    if round(global_line_percent, 2) != EXPECTED_GLOBAL_LINE_PERCENT:
        raise SystemExit(
            f"Se esperaba {EXPECTED_GLOBAL_LINE_PERCENT:.2f}% de líneas globales; "
            f"JaCoCo informa {global_line_percent:.2f}%"
        )
    if failures or errors or skipped:
        raise SystemExit(
            f"Suite no limpia: fallos={failures}, errores={errors}, omitidas={skipped}"
        )
    if len(rows) != 14 or any(row["result"] != "CUBIERTO" for row in rows):
        raise SystemExit("Las 14 unidades objetivo no tienen cobertura completa de líneas y ramas")

    payload = {
        "output": str(OUTPUT),
        "title": "Cobertura dinámica - JaCoCo",
        "subtitle": "US3.1 y US3.2 - ejecución, seguimiento del tiempo y vista personal",
        "rows": rows,
        "us31_lines": story_lines["US3.1"],
        "us32_lines": story_lines["US3.2"],
        "us31_branches": story_branches["US3.1"],
        "us32_branches": story_branches["US3.2"],
        "selected_lines": selected_lines,
        "selected_branches": selected_branches,
        "overall_lines": list(overall_lines),
        "overall_branches": list(counter(report, "BRANCH")),
        "total_tests": total_tests,
        "failures": failures,
        "errors": errors,
        "skipped": skipped,
    }
    with tempfile.NamedTemporaryFile("w", suffix=".json", encoding="utf-8", delete=False) as tmp:
        json.dump(payload, tmp, ensure_ascii=False)
        payload_path = Path(tmp.name)
    try:
        subprocess.run(
            ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(RENDERER), str(payload_path)],
            check=True,
        )
    finally:
        payload_path.unlink(missing_ok=True)
    print(OUTPUT)


if __name__ == "__main__":
    main()
