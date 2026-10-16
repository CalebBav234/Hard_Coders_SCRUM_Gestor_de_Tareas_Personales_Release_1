"""Genera grafos de flujo de control para US1.3 (Override de estados) y US2.1 (Scoping/Isolation)."""
from pathlib import Path
import json
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[3]
OUTPUT = ROOT / "docs" / "pruebas-unitarias" / "evidencias" / "grafos"
RENDERER = Path(__file__).with_name("renderizar_grafos_ciclomaticos_us13_us21.ps1")


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
        "US13-01-CompletionPolicy-canComplete.png", "US-1.3",
        "CompletionPolicy.canComplete", 5, "D + 1 = 4 + 1",
        [
            (1, "if (actor == null ||", True),
            (2, "    !actor.active()) {", True),
            (3, "    return false;", False),
            (4, "}", False),
            (5, "return actor.chief() || assigned;", True),
        ],
        {"I": [970, 250], "P1": [1160, 180], "P2": [1160, 280], "P3": [1160, 380],
         "P4": [1160, 480], "Ft": [1360, 220], "Ff": [1360, 420]},
        [("I", "P1", None, None),
         ("P1", "Ff", "V: actor==null", None), ("P1", "P2", "F: no null", None),
         ("P2", "Ff", "V: !active()", None), ("P2", "P3", "F: active()", None),
         ("P3", "Ft", "V: chief()", None), ("P3", "P4", "F: no chief", None),
         ("P4", "Ft", "V: assigned", None), ("P4", "Ff", "F: !assigned", None)],
        [
            ("B1", "I-P1(V)-Ff", "actor=null", "return false"),
            ("B2", "I-P1(F)-P2(V)-Ff", "actor inactivo", "return false"),
            ("B3", "I-P1(F)-P2(F)-P3(V)-Ft", "Jefe activo", "return true"),
            ("B4", "I-P1(F)-P2(F)-P3(F)-P4(V)-Ft", "Personal asignado", "return true"),
            ("B5", "I-P1(F)-P2(F)-P3(F)-P4(F)-Ff", "ni Jefe ni asignado", "return false"),
        ],
        "5 caminos independientes: actor nulo/inactivo => false; Jefe => true; Personal "
        "asignado => true; el resto => false.",
    ),
    spec(
        "US13-02-CompletionPolicy-requireCanComplete.png", "US-1.3",
        "CompletionPolicy.requireCanComplete", 2, "D + 1 = 1 + 1",
        [
            (1, "if (!canComplete(actor, assigned)) {", True),
            (2, "    throw ForbiddenException;", False),
            (3, "}", False),
        ],
        {"I": [1000, 330], "P": [1180, 330], "F": [1400, 330]},
        [("I", "P", None, None), ("P", "F", "V: !canComplete", None), ("P", "F", "F: canComplete", None)],
        [
            ("B1", "I-P(V)-F", "actor sin permiso", "ForbiddenException (403)"),
            ("B2", "I-P(F)-F", "actor con permiso", "No lanza; transicion continua"),
        ],
        "Guarda usada por start/pause/complete/reopen antes de cualquier cambio de estado.",
    ),
    spec(
        "US13-03-TaskCommandService-transition.png", "US-1.3",
        "TaskCommandService.transition", 3, "D + 1 = 2 + 1",
        [
            (1, "Actor current = actor.current();", False),
            (2, "Task task = load(current, command.id());", False),
            (3, "task.verifyVersion(command.version());", False),
            (4, "boolean assigned = isAssigned(task.id(), current.userId());", False),
            (5, "CompletionPolicy.requireCanComplete(current, assigned);", False),
            (6, "String reason = current.chief() && !assigned", True),
            (7, "    ? overrideReason(command.reason())", True),
            (8, "    : \"Ejecutado por el Personal asignado\";", True),
            (9, "change.accept(task, reason);", False),
            (10, "return result(tasks.save(task));", False),
        ],
        {"I": [960, 240], "P1": [1200, 200], "P2": [1200, 320], "F": [1440, 280]},
        [("I", "P1", None, None),
         ("P1", "F", "V: chief()", None), ("P1", "P2", "F: !chief()", None),
         ("P2", "F", "V: !assigned (override)", None), ("P2", "F", "F: assigned (default)", None)],
        [
            ("B1", "I-P1(V)-P2(V)-F", "Jefe; no asignado", "Override del Jefe (US1.3)"),
            ("B2", "I-P1(V)-P2(F)-F", "Jefe; asignado", "Ejecutado por el Personal asignado"),
            ("B3", "I-P1(F)-F", "Personal no Jefe", "Ejecutado por el Personal asignado"),
        ],
        "El motivo del Jefe solo se aplica cuando el actor es Jefe Y la tarea no esta asignada.",
    ),
    spec(
        "US13-04-TaskCommandService-overrideReason.png", "US-1.3",
        "TaskCommandService.overrideReason", 3, "D + 1 = 2 + 1",
        [
            (1, "String motive = reason == null || reason.isBlank()", True),
            (2, "    ? \"sin motivo indicado\"", True),
            (3, "    : reason.strip();", True),
            (4, "return \"Override del Jefe (US1.3): \" + motive;", False),
        ],
        {"I": [960, 280], "P1": [1200, 220], "P2": [1200, 380], "M": [1400, 190], "S": [1400, 410], "F": [1620, 310]},
        [("I", "P1", None, None),
         ("P1", "M", "V: reason==null", None), ("P1", "P2", "F: no null", None),
         ("P2", "M", "V: isBlank()", None), ("P2", "S", "F: no blank", None),
         ("M", "F", None, None), ("S", "F", None, None)],
        [
            ("B1", "I-P1(V)-M-F", "reason=null", "motive=\"sin motivo indicado\""),
            ("B2", "I-P1(F)-P2(V)-M-F", "reason en blanco", "motive=\"sin motivo indicado\""),
            ("B3", "I-P1(F)-P2(F)-S-F", "reason no vacio", "motive=reason.strip() normalizado"),
        ],
        "Las dos ramas del || producen el motivo por defecto; la tercera normaliza el texto real.",
    ),
    spec(
        "US13-05-TaskCommandService-load.png", "US-1.3",
        "TaskCommandService.load", 4, "D + 1 = 3 + 1",
        [
            (1, "Integer projectId = tasks.projectIdOf(taskId)", False),
            (2, "    .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));", False),
            (3, "if (current.chief()) {", True),
            (4, "    return tasks.findVisible(projectId, taskId)", False),
            (5, "        .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));", False),
            (6, "}", False),
            (7, "Optional<Task> assigned = tasks.findAssignedForUpdate(current.userId(), taskId);", False),
            (8, "if (assigned.isPresent()) {", True),
            (9, "    return assigned.get();", False),
            (10, "}", False),
            (11, "if (tasks.findVisible(projectId, taskId).isPresent()) {", True),
            (12, "    throw new ForbiddenException(\"...Personal asignado...\");", False),
            (13, "}", False),
            (14, "throw new NotFoundException(Task.ENTITY, taskId);", False),
        ],
        {"I": [960, 200], "P1": [1200, 180], "P2": [1200, 310], "P3": [1200, 440],
         "A": [1420, 160], "B": [1420, 310], "C": [1420, 380], "D": [1420, 470]},
        [("I", "P1", None, None),
         ("P1", "A", "V: chief()", None), ("P1", "P2", "F: !chief()", None),
         ("P2", "B", "V: asignada", None), ("P2", "P3", "F: no asignada", None),
         ("P3", "C", "V: visible (403)", None), ("P3", "D", "F: no visible (404)", None)],
        [
            ("B1", "I-P1(V)-A", "Jefe; tarea visible", "return task (sin filtro de asignacion)"),
            ("B2", "I-P1(F)-P2(V)-B", "Personal; tarea asignada", "return assigned task"),
            ("B3", "I-P1(F)-P2(F)-P3(V)-C", "Personal; no asignada; existe", "ForbiddenException (403)"),
            ("B4", "I-P1(F)-P2(F)-P3(F)-D", "Personal; no asignada; inexistente", "NotFoundException (404)"),
        ],
        "AC-US3.2-01: tarea que existe pero no es del actor responde 403, no 404, para no filtrar proyectos ajenos.",
    ),
    spec(
        "US21-01-ProjectAccessPolicy-canView.png", "US-2.1",
        "ProjectAccessPolicy.canView", 5, "D + 1 = 4 + 1",
        [
            (1, "if (actor == null ||", True),
            (2, "    !actor.active()) {", True),
            (3, "    return false;", False),
            (4, "}", False),
            (5, "return actor.chief()", True),
            (6, "    || project.managerUserId().equals(actor.userId());", True),
        ],
        {"I": [960, 250], "P1": [1180, 190], "P2": [1180, 290], "P3": [1180, 390],
         "P4": [1180, 490], "Ft": [1400, 220], "Ff": [1400, 460]},
        [("I", "P1", None, None),
         ("P1", "Ff", "V: actor==null", None), ("P1", "P2", "F: no null", None),
         ("P2", "Ff", "V: !active()", None), ("P2", "P3", "F: active()", None),
         ("P3", "Ft", "V: chief()", None), ("P3", "P4", "F: no chief", None),
         ("P4", "Ft", "V: manager igual", None), ("P4", "Ff", "F: manager distinto", None)],
        [
            ("B1", "I-P1(V)-Ff", "actor=null", "return false"),
            ("B2", "I-P1(F)-P2(V)-Ff", "actor inactivo", "return false"),
            ("B3", "I-P1(F)-P2(F)-P3(V)-Ft", "Jefe", "return true"),
            ("B4", "I-P1(F)-P2(F)-P3(F)-P4(V)-Ft", "Encargado del proyecto", "return true"),
            ("B5", "I-P1(F)-P2(F)-P3(F)-P4(F)-Ff", "otro usuario", "return false"),
        ],
        "5 caminos: Jefe ve todo; Encargado ve los suyos; el resto no ve nada (ni confirma existencia).",
    ),
    spec(
        "US21-02-ProjectAccessPolicy-requireChief.png", "US-2.1",
        "ProjectAccessPolicy.requireChief", 3, "D + 1 = 2 + 1",
        [
            (1, "if (actor == null ||", True),
            (2, "    !actor.isActiveChief()) {", True),
            (3, "    throw ForbiddenException;", False),
            (4, "}", False),
        ],
        {"I": [1020, 320], "P1": [1240, 260], "P2": [1240, 400], "F": [1480, 340]},
        [("I", "P1", None, None),
         ("P1", "F", "V: actor==null", None), ("P1", "P2", "F: no null", None),
         ("P2", "F", "V: !isActiveChief()", None), ("P2", "F", "F: isActiveChief()", [(1360, 470)])],
        [
            ("B1", "I-P1(V)-F", "actor=null", "ForbiddenException (403)"),
            ("B2", "I-P1(F)-P2(V)-F", "actor no Jefe activo", "ForbiddenException (403)"),
            ("B3", "I-P1(F)-P2(F)-F", "Jefe activo", "No lanza; continua"),
        ],
        "Solo Jefe activo pasa; null o no Jefe activo se rechaza con 403 antes de tocar el dominio.",
    ),
    spec(
        "US21-03-ProjectAccessPolicy-requireCanEditDetails.png", "US-2.1",
        "ProjectAccessPolicy.requireCanEditDetails", 2, "D + 1 = 1 + 1",
        [
            (1, "if (!canView(actor, project)) {", True),
            (2, "    throw ForbiddenException;", False),
            (3, "}", False),
        ],
        {"I": [1020, 340], "P": [1240, 340], "F": [1500, 340]},
        [("I", "P", None, None), ("P", "F", "V: !canView", None), ("P", "F", "F: canView", None)],
        [
            ("B1", "I-P(V)-F", "sin permiso de vision", "ForbiddenException (403)"),
            ("B2", "I-P(F)-F", "con permiso de vision", "No lanza; permite editar nombre/descripcion"),
        ],
        "Editar detalles reusa canView: Jefe o Encargado; el resto 403.",
    ),
    spec(
        "US21-04-ProjectQueryService-listProjects.png", "US-2.1",
        "ProjectQueryService.listProjects", 3, "D + 1 = 2 + 1",
        [
            (1, "Actor actor = currentActor.current();", False),
            (2, "if (!actor.active()) {", True),
            (3, "    return List.of();", False),
            (4, "}", False),
            (5, "List<Project> visible = actor.chief()", True),
            (6, "    ? projects.findAll()", True),
            (7, "    : projects.findManagedBy(actor.userId());", True),
            (8, "return assembler.toResults(actor, visible);", False),
        ],
        {"I": [960, 260], "P1": [1180, 220], "P2": [1180, 370], "F": [1440, 310]},
        [("I", "P1", None, None),
         ("P1", "F", "V: !actor.active()", None), ("P1", "P2", "F: actor.active()", None),
         ("P2", "F", "V: chief()", None), ("P2", "F", "F: !chief()", None)],
        [
            ("B1", "I-P1(V)-F", "actor inactivo", "Lista vacia (no revela proyectos ajenos)"),
            ("B2", "I-P1(F)-P2(V)-F", "Jefe activo", "findAll() (proyectos gestionados y delegados)"),
            ("B3", "I-P1(F)-P2(F)-F", "Encargado activo", "findManagedBy(userId) (solo los suyos)"),
        ],
        "El filtrado de existencia se hace en el servidor: inactivos ven vacio y Encargados ven solo sus proyectos.",
    ),
    spec(
        "US21-05-ProjectQueryService-loadVisible.png", "US-2.1",
        "ProjectQueryService.loadVisible", 1, "D + 1 = 0 + 1",
        [
            (1, "return projects.findById(projectId)", False),
            (2, "    .filter(project -> ProjectAccessPolicy.canView(actor, project))", False),
            (3, "    .orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));", False),
        ],
        {"I": [1000, 320], "F": [1420, 320]},
        [("I", "F", None, None)],
        [
            ("B1", "I-F", "findVisible + canView", "Devuelve el proyecto; si no existe o no se puede ver, 404"),
        ],
        "Intraprocedimental D=0: el predicado de visibilidad se delega a canView (US-2.1). "
        "loadVisible solo encadena findById -> filter -> orElseThrow.",
    ),
    spec(
        "US21-06-ProjectCommandService-parseStatus.png", "US-2.1",
        "ProjectCommandService.parseStatus", 3, "D + 1 = 2 + 1",
        [
            (1, "try {", False),
            (2, "    return ProjectStatus.valueOf(", True),
            (3, "        value == null ? \"\" : value.strip().toUpperCase(Locale.ROOT));", True),
            (4, "}", False),
            (5, "catch (IllegalArgumentException ex) {", True),
            (6, "    throw new DomainValidationException(\"...Estado de proyecto desconocido...\");", False),
            (7, "}", False),
        ],
        {"I": [970, 230], "P1": [1220, 210], "P2": [1220, 380], "F": [1500, 320]},
        [("I", "P1", None, None),
         ("P1", "P2", "V: value==null", None), ("P1", "P2", "F: value!=null", None),
         ("P2", "F", "V: valueOf lanza IAE", None), ("P2", "F", "F: valueOf ok", None)],
        [
            ("B1", "I-P1(V)-P2(V)-F", "value=null", "valueOf(\"\") -> IAE -> DomainValidationException (400)"),
            ("B2", "I-P1(F)-P2(F)-F", "value valido", "Devuelve ProjectStatus"),
            ("B3", "I-P1(F)-P2(V)-F", "value invalido", "valueOf -> IAE -> DomainValidationException (400)"),
        ],
        "Normalizacion null-safe: null -> \"\" -> toUpperCase. Un estado desconocido produce 400 (no revela detalle interno).",
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
