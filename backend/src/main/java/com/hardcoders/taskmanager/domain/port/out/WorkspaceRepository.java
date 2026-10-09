package com.hardcoders.taskmanager.domain.port.out;

import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Workspace;

import java.util.List;
import java.util.Optional;

/** Puerto de salida para el espacio de trabajo (proyecto + etapa) y sus actividades. */
public interface WorkspaceRepository {

    /** Espacio de trabajo existente, sin crearlo (para consultas de solo lectura). */
    Optional<Workspace> findWorkspace();

    /** Devuelve el espacio de trabajo, creándolo (con su revisión de plan inicial) si no existe. */
    Workspace currentWorkspace(Integer actorId);

    /** Busca la actividad por nombre (sin distinguir mayúsculas) en la etapa o la crea. */
    Activity findOrCreateActivity(Integer stageId, String name);

    /** Actividades de la etapa ordenadas por nombre. */
    List<Activity> activities(Integer stageId);
}
