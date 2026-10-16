"""Genera PNG de cobertura reproducible para US1.3 (Override) y US2.1 (Isolation) desde JaCoCo/Surefire."""
from pathlib import Path
import xml.etree.ElementTree as ET
import json
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
JACOCO = ROOT / "backend" / "target" / "site" / "jacoco" / "jacoco.xml"
SUREFIRE = ROOT / "backend" / "target" / "surefire-reports"
OUTPUT = Path(__file__).resolve().parents[1] / "evidencias" / "cobertura" / "US13-US21-jacoco-resumen.png"
RENDERER = Path(__file__).with_name("renderizar_evidencia_cobertura_us13_us21.ps1")

# Unidades de las historias US1.3 (Override de estados) y US2.1 (Aislamiento/Scoping).
TARGETS = [
    ("US1.3", "CompletionPolicy", "canComplete"),
    ("US1.3", "CompletionPolicy", "requireCanComplete"),
    ("US1.3", "TaskCommandService", "transition"),
    ("US1.3", "TaskCommandService", "overrideReason"),
    ("US1.3", "TaskCommandService", "load"),
    ("US2.1", "ProjectAccessPolicy", "canView"),
    ("US2.1", "ProjectAccessPolicy", "requireChief"),
    ("US2.1", "ProjectAccessPolicy", "requireCanEditDetails"),
    ("US2.1", "ProjectQueryService", "listProjects"),
    ("US2.1", "ProjectQueryService", "loadVisible"),
    ("US2.1", "ProjectCommandService", "parseStatus"),
]
NEW_TEST_CLASSES = {
    "CompletionPolicyBasicPathTest", "TaskCommandServiceOverrideTest",
    "ProjectAccessPolicyTest", "ProjectQueryServiceScopingTest",
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
story_lines = {"US1.3": [0, 0], "US2.1": [0, 0]}
for story, class_name, method_name in TARGETS:
    if class_name not in classes:
        raise SystemExit(f"No se encontró la clase JaCoCo {class_name}")
    methods = [m for m in classes[class_name].findall("method") if m.get("name") == method_name]
    if len(methods) != 1:
        raise SystemExit(f"Método {class_name}.{method_name}: se esperaba 1 coincidencia, hay {len(methods)}")
    method = methods[0]
    lines = counter(method, "LINE")
    branches = counter(method, "BRANCH")
    story_lines[story][0] += lines[0]
    story_lines[story][1] += lines[1]
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
    "us13_lines": story_lines["US1.3"],
    "us21_lines": story_lines["US2.1"],
    "overall_lines": list(overall_lines),
    "total_tests": total_tests,
    "new_tests": new_tests,
    "failures": failures,
    "errors": errors,
}
renderer = Path(__file__).with_name("renderizar_evidencia_cobertura_us13_us21.ps1")
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
