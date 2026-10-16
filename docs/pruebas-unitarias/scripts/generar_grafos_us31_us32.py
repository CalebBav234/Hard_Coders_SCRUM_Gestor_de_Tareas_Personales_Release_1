"""Genera las láminas de caminos básicos de US3.1 y US3.2."""
from pathlib import Path
import json
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[3]
OUTPUT = ROOT / "docs" / "pruebas-unitarias" / "evidencias" / "grafos"
RENDERER = Path(__file__).with_name("renderizar_grafos_ciclomaticos.ps1")
FOOTER = "Hard Coders - Evidencia US3.1 / US3.2"


def line(number, text, decision=False):
    return (number, text, decision)


def spec(file, story, unit, code, nodes, edges, rows, note):
    return {
        "file": file,
        "story": story,
        "unit": unit,
        "code": code,
        "nodes": nodes,
        "edges": edges,
        "rows": rows,
        "vg": len(rows),
        "formula": f"D + 1 = {len(rows) - 1} + 1",
        "note": note,
        "output": str(OUTPUT / file),
        "footer": FOOTER,
    }


def linear(file, story, unit, code, input_text, output_text, note):
    return spec(
        file, story, unit, code,
        {"I": [1080, 220], "1": [1320, 350], "F": [1570, 500]},
        [("I", "1", None, None), ("1", "F", None, None)],
        [("B1", "I-1-F", input_text, output_text)],
        note,
    )


def binary(file, story, unit, code, condition, true_path, true_result, false_path, false_result, note):
    return spec(
        file, story, unit, code,
        {"I": [970, 190], "D": [1260, 300], "Y": [1080, 500], "N": [1510, 500], "F": [1700, 590]},
        [
            ("I", "D", None, None),
            ("D", "Y", f"V: {condition}", [[1260, 400], [1080, 400]]),
            ("D", "N", "F", [[1260, 430], [1510, 430]]),
            ("Y", "F", None, None),
            ("N", "F", None, None),
        ],
        [
            ("B1", f"I-D(V)-{true_path}-F", condition, true_result),
            ("B2", f"I-D(F)-{false_path}-F", f"no {condition}", false_result),
        ],
        note,
    )


def SPECS():
    specs = []

    specs.append(spec(
        "US31-01-TimeTracking-from.png", "US-3.1", "TimeTracking.from", [
            line(1, "sort changes chronologically;", False),
            line(2, "for (StatusChange change : ordered)", True),
            line(3, "if (change.newStatus() == EN_PROGRESO)", True),
            line(4, "if (openSince == null) openSince = changedAt;", True),
            line(5, "else if (openSince != null) accumulate and close;", True),
            line(6, "current = progress ? openSince : null;", True),
            line(7, "return new TimeTracking(closed, current);", False),
        ],
        {
            "I": [1040, 160], "L": [920, 270], "P": [1100, 270], "O": [1280, 270],
            "W": [1280, 420], "A": [1460, 420], "K": [1640, 420], "M": [1640, 550],
            "C": [1200, 550], "R": [1430, 520], "N": [1430, 620], "F": [1740, 590],
        },
        [
            ("I", "L", None, None), ("L", "P", "V", None),
            ("L", "C", "F", [[900, 400], [1080, 500]]),
            ("P", "O", "V", None), ("P", "A", "F", [[1100, 360], [1460, 360]]),
            ("O", "W", "V", None), ("O", "M", "F", [[1280, 360], [1610, 360], [1610, 510]]),
            ("W", "M", None, None), ("A", "K", "V", None), ("A", "M", "F", None),
            ("K", "M", None, None),
            ("M", "L", "siguiente cambio", [[1830, 550], [1830, 200], [920, 200]]),
            ("C", "R", "V", None), ("C", "N", "F", None),
            ("R", "F", None, None), ("N", "F", None, None),
        ],
        [
            ("B1", "I-L(F)-C(F)-N-F", "changes=[]; estado final=PENDIENTE", "0 segundos; sin segmento abierto"),
            ("B2", "I-L(F)-C(V)-R-F", "changes=[]; estado final=EN_PROGRESO", "Sin inicio deducible; segmento nulo"),
            ("B3", "I-L(V)-P(V)-O(V)-W-L(F)-C(V)-R-F", "Un cambio EN_PROGRESO; estado final en progreso", "Se conserva changedAt como inicio abierto"),
            ("B4", "I-L(V)-P(F)-A(F)-L(F)-C(F)-N-F", "Un cambio a estado no progresivo; sin segmento abierto", "No acumula ni abre tiempo"),
            ("B5", "I-L(V)-P(V)-O(V)-W-L(V)-P(F)-A(V)-L(F)-C(F)-N-F", "Inicio y pausa en orden cronológico", "Cierra y acumula el intervalo"),
            ("B6", "I-L(V)-P(V)-O(F)-L(F)-C(V)-R-F", "Inicio repetido con openSince ya definido", "Mantiene el inicio del primer segmento"),
        ],
        "Bucle de eventos; las cinco decisiones dan V(G)=6.",
    ))

    specs.append(binary(
        "US31-02-TimeTracking-totalSecondsAt.png", "US-3.1", "TimeTracking.totalSecondsAt",
        [line(1, "if (currentSegmentStart == null)", True),
         line(2, "return trackedSeconds;", False),
         line(3, "return trackedSeconds + max(0, elapsed);", False)],
        "inicio nulo", "Y", "Devuelve solo segundos cerrados",
        "N", "Suma el segmento abierto sin tiempo negativo",
        "La condición nula tiene dos caminos: devolver tiempo cerrado o sumar el segmento abierto.",
    ))

    specs.append(binary(
        "US31-03-TimeTracking-toMinutes.png", "US-3.1", "TimeTracking.toMinutes",
        [line(1, "if (seconds <= 0) return null;", True),
         line(2, "return (seconds + 59) / 60;", False)],
        "seconds <= 0", "Y", "Devuelve null",
        "N", "Redondea a 1, 1 o 2 minutos",
        "El redondeo hacia arriba se aplica solo a duraciones positivas.",
    ))

    specs.append(binary(
        "US31-04-Task-transition.png", "US-3.1", "Task.transition",
        [line(1, "if (status != expected)", True),
         line(2, "throw InvalidStatusTransitionException;", False),
         line(3, "status = target; record(previous, target, ...);", False)],
        "status != expected", "Y", "Lanza excepción; no cambia estado",
        "N", "Cambia estado y registra evento",
        "La guarda rechaza cualquier transición cuyo estado actual no sea el esperado.",
    ))

    specs.append(binary(
        "US31-05-Task-refreshActualDuration.png", "US-3.1", "Task.refreshActualDuration",
        [line(1, "seconds = tracking.totalSecondsAt(now);", False),
         line(2, "minutes = TimeTracking.toMinutes(seconds);", False),
         line(3, "if (minutes != null)", True),
         line(4, "actualDurationMinutes = minutes;", False)],
        "minutes != null", "Y", "Actualiza minutos reales",
        "N", "Conserva la duración existente",
        "La guarda evita borrar la duración previa cuando todavía no hay tiempo registrado.",
    ))

    tracking_decisions = [f"átomo {index}" for index in range(1, 5)]
    tracking_code = [
        line(1, "if (trackedSeconds == 0 &&", True),
        line(2, "    currentSegmentStart == null &&", True),
        line(3, "    actualDurationMinutes != null &&", True),
        line(4, "    actualDurationMinutes > 0)", True),
        line(5, "return legacy minutes * 60; otherwise return tracking;", False),
    ]
    tracking_rows = [
        ("B1", "I-D1(F)-R-F", "trackedSeconds != 0", "Devuelve tracking del historial"),
        ("B2", "I-D1(V)-D2(F)-R-F", "trackedSeconds=0; currentSegmentStart != null", "Devuelve tracking abierto"),
        ("B3", "I-D1(V)-D2(V)-D3(F)-R-F", "Sin historial; actualDurationMinutes=null", "Devuelve tracking sin respaldo"),
        ("B4", "I-D1(V)-D2(V)-D3(V)-D4(F)-R-F", "Sin historial; minutos guardados=0", "Devuelve tracking sin respaldo"),
        ("B5", "I-D1(V)-D2(V)-D3(V)-D4(V)-L-F", "Sin historial; minutos guardados>0", "Recupera minutos migrados como segundos"),
    ]
    decisions = {f"D{i}": [960 + (i - 1) * 190, 290] for i in range(1, 5)}
    track_nodes = {"I": [900, 190], **decisions, "R": [1110, 520], "L": [1670, 520], "F": [1670, 620]}
    track_edges = [("I", "D1", None, None)]
    for i in range(1, 5):
        node_name = f"D{i}"
        no_node = "D" + str(i + 1) if i < 4 else "L"
        track_edges.append((node_name, "R", f"F: átomo {i}", [[decisions[node_name][0], 420], [track_nodes["R"][0], 420]]))
        track_edges.append((node_name, no_node, "V", None))
    track_edges.extend([("R", "F", None, None), ("L", "F", None, None)])
    specs.append(spec(
        "US31-06-Task-timeTracking.png", "US-3.1", "Task.timeTracking",
        tracking_code, track_nodes, track_edges, tracking_rows,
        "La condición compuesta tiene cuatro decisiones atómicas; el camino final usa los minutos históricos legados.",
    ))

    specs.append(binary(
        "US31-07-Subtask-transition.png", "US-3.1", "Subtask.transition",
        [line(1, "if (status != expected)", True),
         line(2, "throw InvalidStatusTransitionException;", False),
         line(3, "status = target;", False)],
        "status != expected", "Y", "Lanza excepción; conserva estado",
        "N", "Cambia estado",
        "La subtarea usa la misma guarda de estado que la tarea y no guarda tracking de tiempo propio en V4.",
    ))

    specs.append(spec(
        "US32-01-TaskCommandService-transition.png", "US-3.2", "TaskCommandService.transition",
        [line(1, "current = actor.current(); load + verifyVersion;", False),
         line(2, "assigned = isAssigned(task.id(), current.userId());", False),
         line(3, "requireCanComplete(current, assigned);", False),
         line(4, "reason = current.chief() && !assigned ?", True),
         line(5, "    overrideReason(...) : assignedReason;", True),
         line(6, "change.accept(task, reason); save(task);", False)],
        {"I": [920, 190], "J": [1080, 300], "A": [1280, 300], "P": [1450, 450],
         "O": [1640, 450], "X": [1640, 570], "F": [1780, 620]},
        [
            ("I", "J", None, None), ("J", "P", "F: no es Jefe", [[1080, 400], [1450, 400]]),
            ("J", "A", "V: Jefe", None), ("A", "P", "V: asignada", [[1280, 400], [1450, 400]]),
            ("A", "O", "F: no asignada", None), ("P", "X", "motivo Personal", None),
            ("O", "X", "motivo override", None), ("X", "F", None, None),
        ],
        [
            ("B1", "I-J(F)-P-X-F", "Personal asignado", "Transición autorizada; motivo de Personal"),
            ("B2", "I-J(V)-A(V)-P-X-F", "Jefe; tarea asignada", "Transición autorizada; motivo de Personal"),
            ("B3", "I-J(V)-A(F)-O-X-F", "Jefe; tarea no asignada", "Transición autorizada; motivo de override"),
        ],
        "Las dos condiciones atómicas de current.chief() && !assigned producen tres caminos factibles.",
    ))

    specs.append(linear(
        "US32-02-SubtaskCommandService-execute.png", "US-3.2", "SubtaskCommandService.execute",
        [line(1, "current = actor.current();", False),
         line(2, "subtask = load(current, command.id());", False),
         line(3, "subtask.verifyVersion(command.version());", False),
         line(4, "requireCanComplete(current, isAssignedToTask(...));", False),
         line(5, "change.accept(subtask, defaultReason);", False),
         line(6, "return save(subtask);", False)],
        "Actor, subtarea y versión válidos", "Subtarea actualizada y persistida",
        "Las denegaciones y elementos inexistentes provienen de las políticas y de load; execute no añade decisiones.",
    ))

    load_nodes = {"I": [920, 190], "J": [1080, 290], "C": [1240, 440], "W": [1430, 290],
                  "X": [1620, 440], "R": [1780, 290], "D": [1560, 570], "N": [1740, 570], "F": [1780, 620]}
    load_edges = [
        ("I", "J", None, None), ("J", "C", "Jefe", [[1080, 390], [1240, 390]]),
        ("J", "W", "Personal", None), ("W", "R", "V", None),
        ("W", "X", "F", [[1430, 390], [1620, 390]]),
        ("X", "D", "V", None), ("X", "N", "F", None),
        ("C", "F", None, [[1240, 530], [1780, 530]]), ("R", "F", None, None),
        ("D", "F", "403", None), ("N", "F", "404", None),
    ]
    specs.append(spec(
        "US32-03-SubtaskCommandService-load.png", "US-3.2", "SubtaskCommandService.load",
        [line(1, "taskId = taskIdOf(subtaskId) or 404;", False),
         line(2, "if (current.chief())", True),
         line(3, "return findOpenForUpdate(...) or 404;", False),
         line(4, "assigned = findForUpdate(...).filter(isAssigned);", False),
         line(5, "if (assigned.isPresent()) return assigned;", True),
         line(6, "if (taskIdOf(subtaskId).isPresent()) throw 403;", True),
         line(7, "throw 404;", False)],
        load_nodes, load_edges,
        [
            ("B1", "I-J(V)-C-F", "Jefe; subtarea abierta", "Devuelve subtarea"),
            ("B2", "I-J(F)-W(V)-R-F", "Personal asignado", "Devuelve subtarea"),
            ("B3", "I-J(F)-W(F)-X(V)-D-F", "Personal no asignado; todavía existe", "ForbiddenException (403)"),
            ("B4", "I-J(F)-W(F)-X(F)-N-F", "Elemento eliminado entre lecturas", "NotFoundException (404)"),
        ],
        "Una tarea existente sin asignación produce 403; un identificador inexistente produce 404.",
    ))

    specs.append(linear(
        "US32-04-TaskQueryService-listAssignedTasks.png", "US-3.2", "TaskQueryService.listAssignedTasks",
        [line(1, "actor = currentActor.current();", False),
         line(2, "found = tasks.findAssignedTo(actor.userId(), pattern(query));", False),
         line(3, "return assignedResults(found);", False)],
        "Actor autenticado y filtro opcional", "Lista de tareas asignadas a ese actor",
        "El repositorio restringe la consulta al identificador de usuario autenticado.",
    ))

    specs.append(linear(
        "US32-05-TaskQueryService-toAssigned.png", "US-3.2", "TaskQueryService.toAssigned",
        [line(1, "tracking = task.timeTracking();", False),
         line(2, "copy execution and display fields;", False),
         line(3, "include actual minutes and tracked seconds;", False),
         line(4, "omit budget and estimated duration;", False),
         line(5, "map subtasks and return AssignedTaskResult;", False)],
        "Tarea asignada con campos financieros internos", "AssignedTaskResult sin presupuesto ni estimación",
        "El contrato de proyección personal excluye los campos financieros.",
    ))

    specs.append(linear(
        "US32-06-TaskController-assignedTasks.png", "US-3.2", "TaskController.assignedTasks",
        [line(1, "queryTasks.listAssignedTasks(q);", False),
         line(2, "map each result to AssignedTaskResponse;", False),
         line(3, "return response list;", False)],
        "GET /api/tasks/assigned", "200 OK; JSON sin propiedades financieras",
        "La serialización usa el record AssignedTaskResponse sin presupuesto ni duración estimada.",
    ))

    specs.append(linear(
        "US32-07-TaskController-start.png", "US-3.2", "TaskController.start",
        [line(1, "build TransitionCommand(taskId, reason, version);", False),
         line(2, "manageTasks.start(command);", False),
         line(3, "map TaskResult to TaskResponse;", False)],
        "POST /api/tasks/{id}/start; JSON válido", "200 OK; respuesta con estado EN_PROGRESO",
        "El adaptador es lineal; el caso de uso y GlobalExceptionHandler traducen los errores.",
    ))
    return specs


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    specs = SPECS()
    with tempfile.NamedTemporaryFile("w", suffix=".json", encoding="utf-8", delete=False) as tmp:
        json.dump({"specs": specs}, tmp, ensure_ascii=False)
        payload_path = Path(tmp.name)
    try:
        subprocess.run(
            ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(RENDERER), str(payload_path)],
            check=True,
        )
    finally:
        payload_path.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
