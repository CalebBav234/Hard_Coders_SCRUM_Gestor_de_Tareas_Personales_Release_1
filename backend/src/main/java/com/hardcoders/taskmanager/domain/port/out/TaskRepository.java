package com.hardcoders.taskmanager.domain.port.out;

import com.hardcoders.taskmanager.domain.model.Task;

import java.util.List;
import java.util.Optional;

/** Puerto de salida para persistir tareas con su historial de estados. */
public interface TaskRepository {

    /** Tarea no cancelada del proyecto, bloqueada para modificarla en la transacción actual. */
    Optional<Task> findOpenForUpdate(Integer projectId, Integer taskId);

    Optional<Task> findVisible(Integer projectId, Integer taskId);

    /**
     * Tareas no canceladas del proyecto, de la más reciente a la más antigua.
     *
     * @param likePattern patrón normalizado de {@code SearchText}; nulo para no filtrar
     */
    List<Task> findVisible(Integer projectId, String likePattern);

    /** Tareas COMPLETADAS o CANCELADAS del proyecto (el "historial" de la interfaz). */
    List<Task> findArchived(Integer projectId, String likePattern);

    /** Tareas asignadas al trabajador y no canceladas, de la más reciente a la más antigua (Épica 3). */
    List<Task> findAssignedTo(Integer workerUserId, String likePattern);

    /** Tarea asignada al trabajador y no cancelada, bloqueada para modificarla. */
    Optional<Task> findAssignedForUpdate(Integer workerUserId, Integer taskId);

    /** Proyecto al que pertenece la tarea ({@code task → activity → stage → project}). */
    Optional<Integer> projectIdOf(Integer taskId);

    /** Inserta o actualiza la tarea y agrega sus cambios de estado pendientes; devuelve el estado persistido. */
    Task save(Task task);
}
