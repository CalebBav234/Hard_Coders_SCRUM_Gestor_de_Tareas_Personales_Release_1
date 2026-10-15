"""Genera PNG de cobertura reproducible para US2.2 y US2.3 desde JaCoCo/Surefire."""
from pathlib import Path
import xml.etree.ElementTree as ET
import json
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
JACOCO = ROOT / "backend" / "target" / "site" / "jacoco" / "jacoco.xml"
SUREFIRE = ROOT / "backend" / "target" / "surefire-reports"
OUTPUT = Path(__file__).resolve().parents[1] / "evidencias" / "cobertura" / "US22-US23-jacoco-resumen.png"

# Clases y métodos concretos de las historias, siguiendo el criterio de US11/US12.
TARGETS = [
    ("US2.2", "Stage", "create"),
    ("US2.2", "Activity", "create"),
    ("US2.2", "HierarchyCommandService", "createStage"),
    ("US2.2", "HierarchyCommandService", "createActivity"),
    ("US2.2", "Task", "create"),
    ("US2.2", "TaskCommandService", "create"),
    ("US2.2", "Subtask", "create"),
    ("US2.2", "SubtaskCommandService", "create"),
    ("US2.2", "WbsHierarchyPolicy", "requireSameProject"),
    ("US2.2", "WbsHierarchyPolicy", "requireName"),
    ("US2.2", "HierarchyController", "createStage"),
    ("US2.2", "HierarchyController", "createActivity"),
    ("US2.2", "TaskController", "createTask"),
    ("US2.2", "TaskController", "createSubtask"),
    ("US2.3", "AssignmentPolicy", "requireEligibleWorker"),
    ("US2.3", "AssignmentPolicy", "requireNotSelf"),
    ("US2.3", "AssignmentCommandService", "assign"),
    ("US2.3", "AssignmentCommandService", "unassign"),
    ("US2.3", "AssignmentPersistenceAdapter", "save"),
    ("US2.3", "HierarchyController", "assign"),
    ("US2.3", "HierarchyController", "unassign"),
]
NEW_TEST_CLASSES = {
    "AssignmentCommandServiceCoverageTest", "HierarchyCommandServiceCoverageTest",
    "HierarchyQueryServiceTest", "HierarchyControllerTest", "TaskControllerTest",
    "AssignmentPersistenceAdapterTest", "HierarchyPersistenceAdapterTest",
    "SubtaskPersistenceAdapterCoverageTest",
}


def counter(element, kind):
    found = next((item for item in element.findall("counter") if item.get("type") == kind), None)
    return (0, 0) if found is None else (int(found.get("covered")), int(found.get("missed")))


def pct(pair):
    covered, missed = pair
    return 100.0 if covered + missed == 0 else covered * 100.0 / (covered + missed)


if not JACOCO.exists():
    raise SystemExit("Falta jacoco.xml; ejecuta mvn clean verify desde backend")

report = ET.parse(JACOCO).getroot()
classes = {node.get("name").split("/")[-1]: node for node in report.findall("./package/class")}
rows = []
story_lines = {"US2.2": [0, 0], "US2.3": [0, 0]}
story_branches = {"US2.2": [0, 0], "US2.3": [0, 0]}
for story, class_name, method_name in TARGETS:
    if class_name not in classes:
        raise SystemExit(f"No se encontró la clase JaCoCo {class_name}")
    methods = [m for m in classes[class_name].findall("method") if m.get("name") == method_name]
    if len(methods) != 1:
        raise SystemExit(f"Método {class_name}.{method_name}: se esperaba 1 coincidencia, hay {len(methods)}")
    method = methods[0]
    lines, branches = counter(method, "LINE"), counter(method, "BRANCH")
    for totals, values in ((story_lines[story], lines), (story_branches[story], branches)):
        totals[0] += values[0]
        totals[1] += values[1]
    rows.append((story, f"{class_name}.{method_name}", lines, branches))

total_tests = failures = errors = new_tests = 0
for path in SUREFIRE.glob("TEST-*.xml"):
    suite = ET.parse(path).getroot()
    tests = int(suite.get("tests", 0))
    total_tests += tests
    failures += int(suite.get("failures", 0))
    errors += int(suite.get("errors", 0))
    if path.stem.removeprefix("TEST-").split(".")[-1] in NEW_TEST_CLASSES:
        new_tests += tests
overall_lines = counter(report, "LINE")

image_rows = []
for story, name, lines, branches in rows:
    branch_total = sum(branches)
    if sum(lines) > 0 and lines[1] == 0 and (branch_total == 0 or branches[1] == 0):
        result = "CUBIERTO"
    elif lines[0] == 0:
        result = "SIN COBERTURA"
    else:
        result = "PARCIAL"
    image_rows.append({
        "story": story,
        "method": name,
        "line_covered": lines[0],
        "line_missed": lines[1],
        "branch_covered": branches[0],
        "branch_missed": branches[1],
        "result": result,
    })
payload = {
    "output": str(OUTPUT),
    "rows": image_rows,
    "us22_lines": story_lines["US2.2"],
    "us23_lines": story_lines["US2.3"],
    "overall_lines": list(overall_lines),
    "total_tests": total_tests,
    "new_tests": new_tests,
    "failures": failures,
    "errors": errors,
}
renderer = Path(__file__).with_name("renderizar_evidencia_cobertura.ps1")
with tempfile.NamedTemporaryFile("w", suffix=".json", encoding="utf-8", delete=False) as tmp:
    json.dump(payload, tmp, ensure_ascii=False)
    payload_path = Path(tmp.name)
try:
    subprocess.run(
        ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(renderer), str(payload_path)],
        check=True,
    )
finally:
    payload_path.unlink(missing_ok=True)
print(OUTPUT)
