"""Genera las láminas PNG de caminos básicos para US-1.1 y US-1.2."""

from pathlib import Path
from textwrap import wrap

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "evidencias" / "grafos"
OUTPUT.mkdir(parents=True, exist_ok=True)

W, H = 1920, 1080
BLUE = "#0B6EA8"
LIGHT_BLUE = "#EAF5FB"
ORANGE = "#F6A623"
RED = "#E21B23"
GRAY = "#55585D"
LIGHT_GRAY = "#F4F5F6"
BLACK = "#202124"
WHITE = "#FFFFFF"


def font(size: int, bold: bool = False, mono: bool = False):
    if mono:
        path = r"C:\Windows\Fonts\consola.ttf"
    elif bold:
        path = r"C:\Windows\Fonts\seguisb.ttf"
    else:
        path = r"C:\Windows\Fonts\segoeui.ttf"
    return ImageFont.truetype(path, size)


TITLE = font(42, bold=True)
SUBTITLE = font(24, bold=True)
BODY = font(20)
SMALL = font(17)
TABLE = font(17)
TABLE_BOLD = font(17, bold=True)
CODE = font(20, mono=True)
NODE = font(19, bold=True)


def arrow(draw, start, end, label=None, color=BLACK, width=3, bend=None):
    points = [start] + (bend or []) + [end]
    draw.line(points, fill=color, width=width, joint="curve")
    x1, y1 = points[-2]
    x2, y2 = points[-1]
    dx, dy = x2 - x1, y2 - y1
    length = max((dx * dx + dy * dy) ** 0.5, 1)
    ux, uy = dx / length, dy / length
    px, py = -uy, ux
    size = 12
    tip = (x2, y2)
    left = (x2 - ux * size + px * size * 0.55, y2 - uy * size + py * size * 0.55)
    right = (x2 - ux * size - px * size * 0.55, y2 - uy * size - py * size * 0.55)
    draw.polygon([tip, left, right], fill=color)
    if label:
        mx = (x1 + x2) / 2 + 7
        my = (y1 + y2) / 2 - 22
        draw.text((mx, my), label, font=SMALL, fill=BLUE)


def node(draw, xy, label, radius=27, fill=WHITE):
    x, y = xy
    draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=fill, outline=BLACK, width=3)
    box = draw.textbbox((0, 0), label, font=NODE)
    draw.text((x - (box[2] - box[0]) / 2, y - (box[3] - box[1]) / 2 - 2), label, font=NODE, fill=BLACK)


def code_panel(draw, lines):
    x0, y0, x1, y1 = 55, 150, 870, 625
    draw.rounded_rectangle((x0, y0, x1, y1), radius=18, fill=LIGHT_GRAY, outline="#D3D6DA", width=2)
    draw.text((x0 + 25, y0 + 18), "Código de la unidad", font=SUBTITLE, fill=GRAY)
    y = y0 + 65
    for number, text, decision in lines:
        if number:
            cx = x0 + 30
            draw.ellipse((cx - 15, y - 2, cx + 15, y + 28), fill=ORANGE if decision else "#B7BEC5")
            nbox = draw.textbbox((0, 0), str(number), font=SMALL)
            draw.text((cx - (nbox[2] - nbox[0]) / 2, y + 2), str(number), font=SMALL, fill=WHITE)
        draw.text((x0 + 58, y), text, font=CODE, fill=BLUE)
        y += 37


def graph_panel(draw, nodes, edges, vg, formula):
    draw.text((960, 142), "Grafo de flujo de control", font=SUBTITLE, fill=GRAY)
    for source, target, label, bend in edges:
        arrow(draw, nodes[source], nodes[target], label=label, bend=bend)
    for label, xy in nodes.items():
        node(draw, xy, label, radius=25 if len(label) <= 2 else 30,
             fill=LIGHT_BLUE if label not in {"I", "F"} else WHITE)
    # El recuadro se ubica entre código y grafo, igual que en la lámina de referencia,
    # sin ocultar el nodo final ni las flechas.
    draw.rounded_rectangle((900, 510, 1160, 615), radius=10, fill=WHITE, outline=RED, width=4)
    draw.text((930, 526), f"V(G) = {vg}", font=font(34, bold=True), fill=BLACK)
    draw.text((921, 574), formula, font=SMALL, fill=GRAY)


def table(draw, rows):
    x0, y0, x1 = 55, 675, 1865
    header_h = 46
    row_h = min(58, int((1035 - y0 - header_h) / max(len(rows), 1)))
    widths = [90, 470, 760, 490]
    xs = [x0]
    for width in widths:
        xs.append(xs[-1] + width)
    y1 = y0 + header_h + row_h * len(rows)
    draw.rectangle((x0, y0, x1, y1), fill=WHITE, outline=BLACK, width=2)
    draw.rectangle((x0, y0, x1, y0 + header_h), fill="#E7E9EC", outline=BLACK, width=2)
    for x in xs[1:-1]:
        draw.line((x, y0, x, y1), fill=BLACK, width=2)
    for i in range(len(rows) + 1):
        y = y0 + header_h + i * row_h
        draw.line((x0, y, x1, y), fill=BLACK, width=2)
    headers = ["Nro", "Camino", "Entrada", "Salida esperada"]
    for i, header in enumerate(headers):
        box = draw.textbbox((0, 0), header, font=TABLE_BOLD)
        draw.text(((xs[i] + xs[i + 1] - (box[2] - box[0])) / 2, y0 + 10), header,
                  font=TABLE_BOLD, fill=BLACK)
    for r, row in enumerate(rows):
        y = y0 + header_h + r * row_h
        for c, value in enumerate(row):
            max_chars = [5, 44, 76, 48][c]
            lines = wrap(str(value), max_chars)[:2]
            if c == 0:
                box = draw.textbbox((0, 0), lines[0], font=TABLE)
                tx = (xs[c] + xs[c + 1] - (box[2] - box[0])) / 2
            else:
                tx = xs[c] + 10
            ty = y + max(5, (row_h - len(lines) * 20) / 2)
            for line in lines:
                draw.text((tx, ty), line, font=TABLE, fill=BLACK)
                ty += 20


def render(spec):
    image = Image.new("RGB", (W, H), WHITE)
    draw = ImageDraw.Draw(image)
    draw.text((55, 40), "Caja Blanca – Caminos Básicos", font=TITLE, fill=BLUE)
    draw.text((760, 54), f"{spec['story']} · {spec['unit']}", font=font(27, bold=True), fill=GRAY)
    draw.line((55, 112, 1865, 112), fill="#D7DADD", width=3)
    code_panel(draw, spec["code"])
    graph_panel(draw, spec["nodes"], spec["edges"], spec["vg"], spec["formula"])
    draw.text((55, 638), "Casos derivados de los caminos independientes", font=SUBTITLE, fill=GRAY)
    table(draw, spec["rows"])
    draw.text((1515, 1044), "Hard Coders · Evidencia 2026", font=SMALL, fill="#7B8085")
    path = OUTPUT / spec["file"]
    image.save(path, "PNG", optimize=True)
    print(path)


SPECS = [
    {
        "file": "US11-01-BudgetPolicy-validatePositive.png",
        "story": "US-1.1",
        "unit": "BudgetPolicy.validatePositive",
        "vg": 5,
        "formula": "D + 1 = 4 + 1",
        "code": [
            (1, "if (amount == null ||", True),
            (2, "    amount.signum() < 0) throw;", True),
            (3, "normalized = amount.stripTrailingZeros();", False),
            (4, "if (scale > 2 ||", True),
            (5, "    integerDigits > 13) throw;", True),
            (6, "return;", False),
        ],
        "nodes": {"I": (1040, 205), "1": (1040, 285), "2": (1195, 365), "3": (1390, 285),
                  "4": (1390, 430), "5": (1540, 430), "6": (1690, 430), "F": (1390, 585)},
        "edges": [
            ("I", "1", None, None), ("1", "3", "V", [(1130, 240), (1300, 240)]),
            ("1", "2", "F", None), ("2", "3", "V", None), ("2", "4", "F", [(1195, 430)]),
            ("3", "F", "excepción", [(1480, 350), (1480, 540)]),
            ("4", "F", "V", [(1390, 520)]), ("4", "5", "F", None),
            ("5", "F", "V", [(1540, 520)]), ("5", "6", "F", None), ("6", "F", None, None),
        ],
        "rows": [
            ("B1", "I-1(V)-3-F", "amount = null", "DomainValidationException"),
            ("B2", "I-1(F)-2(V)-3-F", "amount = -0.01", "DomainValidationException"),
            ("B3", "I-1(F)-2(F)-4(V)-F", "amount = 10.001", "DomainValidationException"),
            ("B4", "I-1(F)-2(F)-4(F)-5(V)-F", "amount = 10000000000000.00", "DomainValidationException"),
            ("B5", "I-1(F)-2(F)-4(F)-5(F)-6-F", "amount = 1500.50", "Sin excepción"),
        ],
    },
    {
        "file": "US11-02-BudgetPolicy-validateAllocationCap.png",
        "story": "US-1.1",
        "unit": "BudgetPolicy.validateAllocationCap",
        "vg": 3,
        "formula": "D + 1 = 2 + 1",
        "code": [
            (1, "assigned = allocated == null", True),
            (2, "    ? ZERO : allocated;", False),
            (3, "if (budget.compareTo(assigned) < 0)", True),
            (4, "    throw BudgetBelowAllocationsException;", False),
            (5, "return;", False),
        ],
        "nodes": {"I": (1040, 220), "1": (1160, 310), "2": (1050, 415), "3": (1270, 415),
                  "4": (1430, 500), "5": (1610, 500), "F": (1520, 595)},
        "edges": [
            ("I", "1", None, None), ("1", "2", "V", None), ("1", "3", "F", None),
            ("2", "4", None, None), ("3", "4", None, None), ("4", "F", "V: excepción", None),
            ("4", "5", "F", None), ("5", "F", None, None),
        ],
        "rows": [
            ("B1", "I-1(V)-2-4(F)-5-F", "budget=1.00; allocated=null", "Sin excepción (assigned=0)"),
            ("B2", "I-1(F)-3-4(F)-5-F", "budget=100.00; allocated=100.00", "Sin excepción"),
            ("B3", "I-1(F)-3-4(V)-F", "budget=99.99; allocated=100.00", "BudgetBelowAllocationsException"),
        ],
    },
    {
        "file": "US11-03-ProjectCommandService-create.png",
        "story": "US-1.1",
        "unit": "ProjectCommandService.create",
        "vg": 2,
        "formula": "D + 1 = 1 + 1",
        "code": [
            (1, "actor = currentActor.current();", False),
            (2, "requireChief(actor);", False),
            (3, "managerId == null ? null", True),
            (4, "    : users.findById(managerId);", False),
            (5, "requireEligibleManager(manager);", False),
            (6, "create + save + assemble;", False),
        ],
        "nodes": {"I": (1040, 205), "1": (1040, 290), "2": (1040, 390), "3": (1250, 390),
                  "4": (1450, 300), "5": (1450, 480), "6": (1660, 480), "F": (1660, 585)},
        "edges": [
            ("I", "1", None, None), ("1", "2", None, None), ("2", "3", None, None),
            ("3", "4", "V: null", None), ("4", "F", "rechazo", [(1540, 350), (1660, 390)]),
            ("3", "5", "F: buscar", None), ("5", "6", None, None), ("6", "F", None, None),
        ],
        "rows": [
            ("B1", "I-1-2-3(V)-4-F", "Jefe activo; managerUserId=null", "DomainValidationException; no save"),
            ("B2", "I-1-2-3(F)-5-6-F", "Jefe activo; managerUserId=2 activo", "ProjectResult; revisión 1"),
        ],
    },
    {
        "file": "US11-04-ProjectController-create.png",
        "story": "US-1.1",
        "unit": "ProjectController.create",
        "vg": 1,
        "formula": "D + 1 = 0 + 1",
        "code": [
            (1, "command = new CreateProjectCommand(request...);", False),
            (2, "result = manageProjects.create(command);", False),
            (3, "return ProjectResponse.from(result);", False),
        ],
        "nodes": {"I": (1120, 245), "1": (1270, 330), "2": (1430, 415), "3": (1600, 500), "F": (1750, 585)},
        "edges": [("I", "1", None, None), ("1", "2", None, None), ("2", "3", None, None), ("3", "F", None, None)],
        "rows": [
            ("B1", "I-1-2-3-F", "CreateProjectRequest válido", "HTTP 201 + ProjectResponse"),
        ],
    },
    {
        "file": "US12-01-DelegationPolicy-canDelegate.png",
        "story": "US-1.2",
        "unit": "DelegationPolicy.canDelegate",
        "vg": 3,
        "formula": "D + 1 = 2 + 1",
        "code": [
            (1, "return delegator != null &&", True),
            (2, "       delegator.isActiveChief();", True),
        ],
        "nodes": {"I": (1070, 220), "1": (1190, 315), "2": (1420, 315), "3": (1320, 500),
                  "4": (1620, 500), "F": (1470, 595)},
        "edges": [
            ("I", "1", None, None), ("1", "3", "F", None), ("1", "2", "V", None),
            ("2", "3", "F", None), ("2", "4", "V", None), ("3", "F", "false", None),
            ("4", "F", "true", None),
        ],
        "rows": [
            ("B1", "I-1(F)-3-F", "delegator = null", "false"),
            ("B2", "I-1(V)-2(F)-3-F", "Actor(1, chief=true, active=false)", "false"),
            ("B3", "I-1(V)-2(V)-4-F", "Actor(1, chief=true, active=true)", "true"),
        ],
    },
    {
        "file": "US12-02-ProjectCommandService-delegate.png",
        "story": "US-1.2",
        "unit": "ProjectCommandService.delegate",
        "vg": 2,
        "formula": "D + 1 = 1 + 1",
        "code": [
            (1, "actor + project = loadVisible(...);", False),
            (2, "requireCanDelegate(actor); verifyVersion();", False),
            (3, "managerId == null ? null", True),
            (4, "    : users.findById(managerId);", False),
            (5, "validate + delegateTo + save;", False),
            (6, "return ProjectResult;", False),
        ],
        "nodes": {"I": (1040, 205), "1": (1040, 300), "2": (1040, 410), "3": (1260, 410),
                  "4": (1460, 310), "5": (1460, 500), "6": (1660, 500), "F": (1660, 600)},
        "edges": [
            ("I", "1", None, None), ("1", "2", None, None), ("2", "3", None, None),
            ("3", "4", "V: null", None), ("4", "F", "rechazo", [(1550, 360), (1660, 410)]),
            ("3", "5", "F: buscar", None), ("5", "6", None, None), ("6", "F", None, None),
        ],
        "rows": [
            ("B1", "I-1-2-3(V)-4-F", "Jefe; proyecto visible; managerUserId=null", "DomainValidationException; no save"),
            ("B2", "I-1-2-3(F)-5-6-F", "Jefe; versión válida; Encargado 3 activo", "ProjectResult; revisión auditada"),
        ],
    },
]


if __name__ == "__main__":
    for item in SPECS:
        render(item)
