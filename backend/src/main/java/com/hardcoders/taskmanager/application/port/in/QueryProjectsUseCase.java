package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.result.PlanRevisionResult;
import com.hardcoders.taskmanager.application.result.ProjectResult;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;

import java.util.List;

/** Consultas de proyectos filtradas en el servidor según el actor. */
public interface QueryProjectsUseCase {

    /** Jefe: todos; Encargado: los que dirige; otros: ninguno. */
    List<ProjectResult> listProjects();

    ProjectResult getProject(Integer projectId);

    List<PlanRevisionResult> planHistory(Integer projectId);

    /** Usuarios activos a los que el Jefe puede delegar (solo Jefe). */
    List<UserSummaryResult> eligibleManagers();
}
