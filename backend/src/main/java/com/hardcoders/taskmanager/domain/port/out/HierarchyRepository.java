package com.hardcoders.taskmanager.domain.port.out;

import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Stage;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la jerarquía Proyecto → Etapa → Actividad de V004.
 * Sustituye al antiguo "espacio de trabajo": ya no se selecciona el primer proyecto
 * (fallback eliminado, US2.2); toda operación indica el proyecto, la etapa o la actividad.
 */
public interface HierarchyRepository {

    Optional<Stage> findStage(Integer stageId);

    /** Etapa bloqueada para modificarla en la transacción actual. */
    Optional<Stage> findStageForUpdate(Integer stageId);

    /** Etapas del proyecto, en orden de creación. */
    List<Stage> stagesOf(Integer projectId);

    Stage save(Stage stage);

    Optional<Activity> findActivity(Integer activityId);

    /** Actividad bloqueada para modificarla en la transacción actual. */
    Optional<Activity> findActivityForUpdate(Integer activityId);

    /** Actividades de la etapa, en orden alfabético. */
    List<Activity> activitiesOf(Integer stageId);

    Activity save(Activity activity);
}
