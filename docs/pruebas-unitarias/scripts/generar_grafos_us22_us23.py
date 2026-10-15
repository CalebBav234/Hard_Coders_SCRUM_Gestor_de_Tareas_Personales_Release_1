"""Genera grafos de flujo de control para US2.2 y US2.3 sin tocar las láminas existentes."""
from pathlib import Path
import json
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[3]
OUTPUT = ROOT / "docs" / "pruebas-unitarias" / "evidencias" / "grafos"
RENDERER = Path(__file__).with_name("renderizar_grafos_ciclomaticos.ps1")


def spec(file, story, unit, vg, formula, code, nodes, edges, rows, note=None):
    return {
        "file": file,
        "story": story,
        "unit": unit,
        "vg": vg,
        "formula": formula,
        "code": code,
        "nodes": nodes,
        "edges": edges,
        "rows": rows,
        "note": note or "Caminos independientes derivados del flujo del metodo.",
        "output": str(OUTPUT / file),
    }


SPECS = [
    spec(
        "US22-01-WbsHierarchyPolicy-requireSameProject.png", "US-2.2",
        "WbsHierarchyPolicy.requireSameProject", 3, "D + 1 = 2 + 1",
        [
            (1, "if (actualProjectId == null ||", True),
            (2, "    !actualProjectId.equals(expectedProjectId))", True),
            (3, "    throw NotFoundException;", False),
            (4, "return;", False),
        ],
        {"I": [1010, 210], "1": [1160, 300], "2": [1400, 300], "3": [1400, 470], "4": [1630, 470], "F": [1630, 590]},
        [
            ("I", "1", None, None), ("1", "3", "V: null", None), ("1", "2", "F", None),
            ("2", "3", "V: distinto", None), ("2", "4", "F: igual", None), ("3", "F", "404", None),
            ("4", "F", None, None),
        ],
        [
            ("B1", "I-1(V)-3-F", "actualProjectId=null", "NotFoundException (404)"),
            ("B2", "I-1(F)-2(V)-3-F", "expected=7; actual=9", "NotFoundException (404)"),
            ("B3", "I-1(F)-2(F)-4-F", "expected=7; actual=7", "Continua sin excepci\u00f3n"),
        ],
    ),
    spec(
        "US22-02-WbsHierarchyPolicy-requireName.png", "US-2.2",
        "WbsHierarchyPolicy.requireName", 4, "D + 1 = 3 + 1",
        [
            (1, "if (value == null ||", True),
            (2, "    value.strip().isEmpty())", True),
            (3, "    throw DomainValidationException;", False),
            (4, "String stripped = value.strip();", False),
            (5, "if (stripped.length() > maxLength)", True),
            (6, "    throw DomainValidationException;", False),
            (7, "return stripped;", False),
        ],
        {"I": [990, 190], "1": [1100, 270], "2": [1300, 270], "3": [1300, 410], "4": [1500, 390],
         "5": [1500, 500], "6": [1640, 565], "7": [1690, 390], "F": [1690, 590]},
        [
            ("I", "1", None, None), ("1", "3", "V: null", None), ("1", "2", "F", None),
            ("2", "3", "V: vacio", None), ("2", "4", "F", None), ("3", "F", "400", None),
            ("4", "5", None, None), ("5", "6", "V: excede", None), ("5", "7", "F", None),
            ("6", "F", "400", None), ("7", "F", None, None),
        ],
        [
            ("B1", "I-1(V)-3-F", "value=null", "DomainValidationException (400)"),
            ("B2", "I-1(F)-2(V)-3-F", "value=\"   \"", "DomainValidationException (400)"),
            ("B3", "I-1(F)-2(F)-4-5(V)-6-F", "161 caracteres; maxLength=160", "DomainValidationException (400)"),
            ("B4", "I-1(F)-2(F)-4-5(F)-7-F", "value=\"  Fase 1  \"", "Devuelve \"Fase 1\""),
        ],
        "Incluye la ruta de longitud maxima como caso propuesto; verificarla en pruebas automatizadas.",
    ),
    spec(
        "US22-03-HierarchyCommandService-createStage.png", "US-2.2",
        "HierarchyCommandService.createStage", 1, "D + 1 = 0 + 1",
        [
            (1, "actor = currentActor.current();", False),
            (2, "project = loadManageable(actor, projectId);", False),
            (3, "requireOpen(project);", False),
            (4, "name = requireName(command.name());", False),
            (5, "description = requireDescription(...);", False),
            (6, "stage = Stage.create(project.id(), ...);", False),
            (7, "return toResult(hierarchy.save(stage));", False),
        ],
        {"I": [1000, 205], "1": [1080, 285], "2": [1200, 365], "3": [1360, 285], "4": [1490, 365],
         "5": [1630, 285], "6": [1630, 440], "7": [1480, 520], "F": [1480, 600]},
        [("I", "1", None, None), ("1", "2", None, None), ("2", "3", None, None), ("3", "4", None, None),
         ("4", "5", None, None), ("5", "6", None, None), ("6", "7", None, None), ("7", "F", None, None)],
        [("B1", "I-1-2-3-4-5-6-7-F", "Encargado autorizado; proyecto abierto; nombre valido", "Etapa guardada; StageResult")],
    ),
    spec(
        "US22-04-HierarchyCommandService-createActivity.png", "US-2.2",
        "HierarchyCommandService.createActivity", 1, "D + 1 = 0 + 1",
        [
            (1, "actor = currentActor.current();", False),
            (2, "stage = hierarchy.findStage(stageId);", False),
            (3, "project = requireManageableProject(actor, stage);", False),
            (4, "requireOpen(project);", False),
            (5, "name = requireName(command.name());", False),
            (6, "description = requireDescription(...);", False),
            (7, "activity = Activity.create(stage.id(), ...);", False),
            (8, "return toResult(hierarchy.save(activity));", False),
        ],
        {"I": [990, 190], "1": [1070, 265], "2": [1180, 340], "3": [1320, 265], "4": [1450, 340],
         "5": [1570, 265], "6": [1690, 340], "7": [1690, 485], "8": [1530, 535], "F": [1530, 605]},
        [("I", "1", None, None), ("1", "2", None, None), ("2", "3", None, None), ("3", "4", None, None),
         ("4", "5", None, None), ("5", "6", None, None), ("6", "7", None, None), ("7", "8", None, None),
         ("8", "F", None, None)],
        [("B1", "I-1-2-3-4-5-6-7-8-F", "Etapa del proyecto; Encargado autorizado; nombre valido", "Actividad guardada; ActivityResult")],
    ),
    spec(
        "US22-05-TaskCommandService-create.png", "US-2.2",
        "TaskCommandService.create", 1, "D + 1 = 0 + 1",
        [
            (1, "current = actor.current();", False),
            (2, "activity = hierarchy.findActivity(activityId);", False),
            (3, "project = requireManageableProject(current, stageId);", False),
            (4, "requireOpen(project);", False),
            (5, "task = Task.create(title, priority, activity, ...);", False),
            (6, "return result(tasks.save(task));", False),
        ],
        {"I": [1010, 210], "1": [1090, 290], "2": [1210, 365], "3": [1370, 290], "4": [1510, 365],
         "5": [1650, 290], "6": [1650, 465], "F": [1510, 575]},
        [("I", "1", None, None), ("1", "2", None, None), ("2", "3", None, None), ("3", "4", None, None),
         ("4", "5", None, None), ("5", "6", None, None), ("6", "F", None, None)],
        [("B1", "I-1-2-3-4-5-6-F", "Actividad valida; proyecto abierto; titulo y datos permitidos", "Tarea guardada; TaskResult")],
    ),
    spec(
        "US22-06-SubtaskCommandService-create.png", "US-2.2",
        "SubtaskCommandService.create", 1, "D + 1 = 0 + 1",
        [
            (1, "current = actor.current();", False),
            (2, "task = loadManageableTask(current, taskId);", False),
            (3, "subtask = Subtask.create(task.id(), title, ...);", False),
            (4, "return toResult(subtasks.save(subtask));", False),
        ],
        {"I": [1030, 210], "1": [1150, 300], "2": [1320, 390], "3": [1500, 300], "4": [1660, 390], "F": [1660, 550]},
        [("I", "1", None, None), ("1", "2", None, None), ("2", "3", None, None), ("3", "4", None, None), ("4", "F", None, None)],
        [("B1", "I-1-2-3-4-F", "Encargado autorizado; tarea gestionable; titulo valido", "Subtarea guardada; SubtaskResult")],
    ),
    spec(
        "US23-01-AssignmentPolicy-requireEligibleWorker.png", "US-2.3",
        "AssignmentPolicy.requireEligibleWorker", 3, "D + 1 = 2 + 1",
        [
            (1, "if (worker == null)", True),
            (2, "    throw DomainValidationException;", False),
            (3, "if (!worker.active())", True),
            (4, "    throw DomainValidationException;", False),
            (5, "return;", False),
        ],
        {"I": [1020, 210], "1": [1160, 300], "2": [1160, 455], "3": [1400, 300], "4": [1540, 455], "5": [1660, 300], "F": [1660, 570]},
        [("I", "1", None, None), ("1", "2", "V", None), ("1", "3", "F", None), ("2", "F", "400", None),
         ("3", "4", "V: inactivo", None), ("3", "5", "F: activo", None), ("4", "F", "400", None), ("5", "F", None, None)],
        [
            ("B1", "I-1(V)-2-F", "worker=null", "DomainValidationException (400)"),
            ("B2", "I-1(F)-3(V)-4-F", "worker.active=false", "DomainValidationException (400)"),
            ("B3", "I-1(F)-3(F)-5-F", "worker.active=true", "Continua sin excepci\u00f3n"),
        ],
        "La suite actual prueba el trabajador inactivo; los otros caminos quedan como casos de la lamina.",
    ),
    spec(
        "US23-02-AssignmentPolicy-requireNotSelf.png", "US-2.3",
        "AssignmentPolicy.requireNotSelf", 4, "D + 1 = 3 + 1",
        [
            (1, "if (actor != null &&", True),
            (2, "    worker != null &&", True),
            (3, "    worker.userId().equals(actor.userId()))", True),
            (4, "    throw ForbiddenException;", False),
            (5, "return;", False),
        ],
        {"I": [990, 195], "1": [1100, 275], "2": [1280, 275], "3": [1480, 275], "4": [1480, 455], "5": [1690, 455], "F": [1690, 585]},
        [("I", "1", None, None), ("1", "5", "F", None), ("1", "2", "V", None),
         ("2", "5", "F", None), ("2", "3", "V", None), ("3", "4", "V", None),
         ("3", "5", "F", None), ("4", "F", "403", None), ("5", "F", None, None)],
        [
            ("B1", "I-1(F)-5-F", "actor=null; worker activo", "Continua sin excepci\u00f3n"),
            ("B2", "I-1(V)-2(F)-5-F", "actor activo; worker=null", "Continua sin excepci\u00f3n"),
            ("B3", "I-1(V)-2(V)-3(F)-5-F", "IDs distintos", "Continua sin excepci\u00f3n"),
            ("B4", "I-1(V)-2(V)-3(V)-4-F", "IDs iguales", "ForbiddenException (403)"),
        ],
        "La ruta de autoasignacion se presenta como caso esperado; confirmar que exista una prueba automatizada.",
    ),
    spec(
        "US23-03-AssignmentCommandService-assign.png", "US-2.3",
        "AssignmentCommandService.assign", 4, "D + 1 = 3 + 1",
        [
            (1, "actor/task/project = load context;", False),
            (2, "requireOpen + requireCanAssign;", False),
            (3, "worker = users.findById(workerId);", False),
            (4, "requireEligibleWorker + requireNotSelf;", False),
            (5, "if (assignment already exists)", True),
            (6, "    throw DomainValidationException;", False),
            (7, "if (command.reason() == null ||", True),
            (8, "    command.reason().isBlank())", True),
            (9, "    ? default reason", False),
            (10, "    : command.reason().strip();", False),
            (11, "save TaskAssignment; return result;", False),
        ],
        {"I": [970, 180], "1": [1030, 240], "2": [1130, 300], "3": [1230, 240], "4": [1330, 300],
         "5": [1430, 240], "6": [1430, 400], "7": [1540, 310], "8": [1660, 240],
         "9": [1630, 415], "10": [1780, 350], "11": [1640, 515], "F": [1640, 600]},
        [("I", "1", None, None), ("1", "2", None, None), ("2", "3", None, None), ("3", "4", None, None),
         ("4", "5", None, None), ("5", "6", "V", None), ("5", "7", "F", None),
         ("6", "F", None, None), ("7", "9", "V", [(1540, 390)]), ("7", "8", "F", None),
         ("8", "9", "V", None), ("8", "10", "F", None),
         ("9", "11", None, None), ("10", "11", None, [(1750, 500)]), ("11", "F", None, None)],
        [
            ("B1", "I-1-2-3-4-5(V)-6-F", "Asignacion previa para la pareja tarea/trabajador", "DomainValidationException"),
            ("B2", "I-1-2-3-4-5(F)-7(V)-9-11-F", "Sin asignacion previa; reason=null", "Guarda motivo por defecto"),
            ("B3", "I-1-2-3-4-5(F)-7(F)-8(V)-9-11-F", "Sin asignacion previa; reason=\"   \"", "Guarda motivo por defecto"),
            ("B4", "I-1-2-3-4-5(F)-7(F)-8(F)-10-11-F", "Sin asignacion previa; reason=\"Refuerzo\"", "Guarda motivo normalizado"),
        ],
        "El caso duplicado mostrado es el rechazo preventivo del servicio; el conflicto de clave concurrente se maneja aparte.",
    ),
]


if __name__ == "__main__":
    OUTPUT.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", suffix=".json", encoding="utf-8", delete=False) as tmp:
        json.dump({"specs": SPECS}, tmp, ensure_ascii=False)
        payload_path = Path(tmp.name)
    try:
        subprocess.run(
            ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(RENDERER), str(payload_path)],
            check=True,
        )
    finally:
        payload_path.unlink(missing_ok=True)
