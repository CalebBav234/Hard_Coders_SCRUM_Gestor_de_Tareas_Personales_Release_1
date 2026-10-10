package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.StageResult;
import com.hardcoders.taskmanager.application.result.TaskResult;

import java.util.List;

/** Puerto de entrada: consultas de la jerarquía, filtradas por proyecto visible para el actor. */
public interface QueryHierarchyUseCase {

    /** Etapas del proyecto (solo si el actor puede verlo). */
    List<StageResult> stagesOf(Integer projectId);

    /** Actividades de la etapa (solo si la etapa pertenece a un proyecto visible). */
    List<ActivityResult> activitiesOf(Integer stageId);

    /** Tareas no canceladas de la actividad (solo si pertenece a un proyecto visible). */
    List<TaskResult> tasksOf(Integer activityId);
}
