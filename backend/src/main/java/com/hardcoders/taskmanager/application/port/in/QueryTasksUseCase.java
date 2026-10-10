package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.result.AssignedTaskResult;
import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskHistoryResult;
import com.hardcoders.taskmanager.application.result.TaskResult;

import java.util.List;

/** Puerto de entrada: consultas de solo lectura. */
public interface QueryTasksUseCase {

    /**
     * Tareas visibles del proyecto (no canceladas) con sus subtareas.
     *
     * @param projectId proyecto obligatorio: ya no se deduce del primer proyecto
     * @param query     filtro por título o descripción
     */
    List<TaskResult> listTasks(Integer projectId, String query);

    TaskResult getTask(Integer taskId);

    /** Tareas completadas o canceladas del proyecto, con su historial de estados. */
    List<TaskHistoryResult> taskHistory(Integer projectId, String query);

    /** Subtareas completadas o canceladas del proyecto. */
    List<SubtaskResult> subtaskHistory(Integer projectId, String query);

    /** Actividades ("categorías") de la etapa, sin "Sin categoría". */
    List<ActivityResult> activities(Integer stageId);

    /** US3.1: tareas asignadas al usuario que inició sesión, sin datos financieros. */
    List<AssignedTaskResult> listAssignedTasks(String query);

    /** US3.1: una tarea asignada al usuario que inició sesión. */
    AssignedTaskResult getAssignedTask(Integer taskId);

    /** Subtareas de una tarea visible para el actor. */
    List<SubtaskResult> subtasksOfTask(Integer taskId);
}
