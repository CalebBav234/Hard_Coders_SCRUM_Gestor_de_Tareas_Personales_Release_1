package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskHistoryResult;
import com.hardcoders.taskmanager.application.result.TaskResult;

import java.util.List;

/** Puerto de entrada: consultas de solo lectura. */
public interface QueryTasksUseCase {

    /** Tareas visibles (no canceladas) con sus subtareas; {@code query} filtra por título o descripción. */
    List<TaskResult> listTasks(String query);

    TaskResult getTask(Integer taskId);

    /** Tareas completadas o canceladas con su historial de estados. */
    List<TaskHistoryResult> taskHistory(String query);

    /** Subtareas completadas o canceladas. */
    List<SubtaskResult> subtaskHistory(String query);

    /** Actividades ("categorías") del espacio de trabajo, sin "Sin categoría". */
    List<ActivityResult> activities();
}
