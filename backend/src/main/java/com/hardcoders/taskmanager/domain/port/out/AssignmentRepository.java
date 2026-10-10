package com.hardcoders.taskmanager.domain.port.out;

import com.hardcoders.taskmanager.domain.model.TaskAssignment;

import java.util.List;
import java.util.Optional;

/** Puerto de salida para las asignaciones Personal ↔ Tarea (V004 {@code task_assignment}). */
public interface AssignmentRepository {

    Optional<TaskAssignment> find(Integer taskId, Integer workerUserId);

    /** Asignaciones vigentes de la tarea, del trabajador más reciente al más antiguo. */
    List<TaskAssignment> findForTask(Integer taskId);

    /** Asignaciones vigentes de varias tareas, indexadas por {@code task_id}. */
    java.util.Map<Integer, List<TaskAssignment>> findForTasks(java.util.Collection<Integer> taskIds);

    /** Asignaciones vigentes del trabajador (vista del Personal). */
    List<TaskAssignment> findForWorker(Integer workerUserId);

    /** Inserta una asignación; la duplicidad la rechaza la clave compuesta de V004. */
    TaskAssignment save(TaskAssignment assignment);

    /** Elimina una asignación (revocación). */
    void delete(Integer taskId, Integer workerUserId);
}
