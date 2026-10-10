package com.hardcoders.taskmanager.domain.port.out;

import com.hardcoders.taskmanager.domain.model.Subtask;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Puerto de salida para subtareas. */
public interface SubtaskRepository {

    /** Subtarea no cancelada cuya tarea pertenece al proyecto, bloqueada para modificarla. */
    Optional<Subtask> findOpenForUpdate(Integer projectId, Integer subtaskId);

    /** Subtarea no cancelada de la tarea indicada, bloqueada para modificarla. */
    Optional<Subtask> findForUpdate(Integer taskId, Integer subtaskId);

    /** Identificador de la tarea contenedora de la subtarea. */
    Optional<Integer> taskIdOf(Integer subtaskId);

    /** Subtareas no canceladas de las tareas indicadas, en orden de creación. */
    List<Subtask> findVisibleByTaskIds(Collection<Integer> taskIds);

    /** Subtareas COMPLETADAS o CANCELADAS del proyecto (el "historial" de la interfaz). */
    List<Subtask> findArchived(Integer projectId, String likePattern);

    boolean hasOpenSubtasks(Integer taskId);

    Subtask save(Subtask subtask);
}
