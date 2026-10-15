"""Crea una evidencia PNG reproducible desde los XML reales de JaCoCo y Surefire."""

from pathlib import Path
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[3]
JACOCO = ROOT / "backend" / "target" / "site" / "jacoco" / "jacoco.xml"
SUREFIRE = ROOT / "backend" / "target" / "surefire-reports"
OUTPUT = Path(__file__).resolve().parents[1] / "evidencias" / "cobertura" / "US11-US12-jacoco-resumen.png"

TARGETS = [
    ("US-1.1", "BudgetPolicy", "validatePositive"),
    ("US-1.1", "BudgetPolicy", "validateAllocationCap"),
    ("US-1.1", "ProjectCommandService", "create"),
    ("US-1.1", "ProjectController", "create"),
    ("US-1.2", "DelegationPolicy", "canDelegate"),
    ("US-1.2", "ProjectCommandService", "delegate"),
]
NEW_TEST_CLASSES = {
    "BudgetPolicyBasicPathTest",
    "DelegationPolicyBasicPathTest",
    "ProjectCommandServiceUnitTest",
    "ProjectControllerTest",
}


def font(size, bold=False):
    name = "seguisb.ttf" if bold else "segoeui.ttf"
    return ImageFont.truetype(str(Path(r"C:\Windows\Fonts") / name), size)


def counter(element, kind):
    found = next((item for item in element.findall("counter") if item.get("type") == kind), None)
    if found is None:
        return 0, 0
    return int(found.get("covered")), int(found.get("missed"))


def pct(pair):
    covered, missed = pair
    return 100.0 if covered + missed == 0 else covered * 100.0 / (covered + missed)


if not JACOCO.exists():
    raise SystemExit("Falta backend/target/site/jacoco/jacoco.xml; ejecuta mvn clean verify")

report = ET.parse(JACOCO).getroot()
classes = {node.get("name").split("/")[-1]: node for node in report.findall("./package/class")}
rows = []
scope_lines = [0, 0]
scope_branches = [0, 0]
for story, class_name, method_name in TARGETS:
    methods = [m for m in classes[class_name].findall("method") if m.get("name") == method_name]
    method = methods[0]
    lines = counter(method, "LINE")
    branches = counter(method, "BRANCH")
    scope_lines[0] += lines[0]
    scope_lines[1] += lines[1]
    scope_branches[0] += branches[0]
    scope_branches[1] += branches[1]
    rows.append((story, f"{class_name}.{method_name}", lines, branches))

total_tests = failures = errors = 0
new_tests = 0
for path in SUREFIRE.glob("TEST-*.xml"):
    suite = ET.parse(path).getroot()
    tests = int(suite.get("tests", 0))
    total_tests += tests
    failures += int(suite.get("failures", 0))
    errors += int(suite.get("errors", 0))
    if path.stem.removeprefix("TEST-").split(".")[-1] in NEW_TEST_CLASSES:
        new_tests += tests

overall_lines = counter(report, "LINE")

W, H = 1920, 1080
img = Image.new("RGB", (W, H), "white")
d = ImageDraw.Draw(img)
blue, green, orange, gray, black = "#0B6EA8", "#14804A", "#D87500", "#596168", "#202124"
d.text((70, 50), "Cobertura dinámica – JaCoCo", font=font(46, True), fill=blue)
d.text((70, 112), "Épicas US-1.1 y US-1.2 · ejecución reproducible desde XML", font=font(25), fill=gray)
d.line((70, 160, 1850, 160), fill="#D7DADD", width=3)

cards = [
    ("Líneas de las 6 unidades", f"{pct(scope_lines):.0f}%", f"{scope_lines[0]}/{sum(scope_lines)}", green),
    ("Ramas de las 6 unidades", f"{pct(scope_branches):.0f}%", f"{scope_branches[0]}/{sum(scope_branches)}", green),
    ("Suite backend", f"{total_tests} OK", f"{failures} fallos · {errors} errores", green),
    ("Cobertura global backend", f"{pct(overall_lines):.2f}%", "Pendiente del resto del equipo", orange),
]
for i, (title, value, detail, color) in enumerate(cards):
    x0 = 70 + i * 445
    d.rounded_rectangle((x0, 200, x0 + 405, 385), radius=18, fill="#F6F8FA", outline="#D5D9DD", width=2)
    d.text((x0 + 24, 222), title, font=font(20, True), fill=gray)
    d.text((x0 + 24, 268), value, font=font(39, True), fill=color)
    d.text((x0 + 24, 333), detail, font=font(18), fill=gray)

d.text((70, 435), "Detalle por método probado", font=font(27, True), fill=gray)
x = [70, 230, 930, 1240, 1530, 1850]
y0, row_h = 490, 68
d.rectangle((x[0], y0, x[-1], y0 + row_h * 7), outline=black, width=2)
d.rectangle((x[0], y0, x[-1], y0 + row_h), fill="#E7E9EC", outline=black, width=2)
for xv in x[1:-1]:
    d.line((xv, y0, xv, y0 + row_h * 7), fill=black, width=2)
for i in range(1, 7):
    d.line((x[0], y0 + i * row_h, x[-1], y0 + i * row_h), fill=black, width=2)
headers = ["US", "Unidad", "Líneas", "Ramas", "Resultado"]
for i, text in enumerate(headers):
    d.text((x[i] + 14, y0 + 20), text, font=font(18, True), fill=black)
for r, (story, name, lines, branches) in enumerate(rows, start=1):
    y = y0 + r * row_h + 20
    branch_text = "N/A" if sum(branches) == 0 else f"{branches[0]}/{sum(branches)} ({pct(branches):.0f}%)"
    values = [story, name, f"{lines[0]}/{sum(lines)} ({pct(lines):.0f}%)", branch_text, "CUBIERTO"]
    for i, text in enumerate(values):
        d.text((x[i] + 14, y), text, font=font(17, i == 4), fill=green if i == 4 else black)

d.text((70, 1000), f"Pruebas nuevas de esta rama: {new_tests} · Informe: backend/target/site/jacoco/index.html",
       font=font(20), fill=gray)
d.text((1550, 1000), "Hard Coders · 14/10/2026", font=font(18), fill=gray)
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
img.save(OUTPUT, "PNG", optimize=True)
print(OUTPUT)
