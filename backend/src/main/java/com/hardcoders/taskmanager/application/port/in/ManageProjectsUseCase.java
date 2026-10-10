package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.command.ChangeProjectStatusCommand;
import com.hardcoders.taskmanager.application.command.CreateProjectCommand;
import com.hardcoders.taskmanager.application.command.DelegateProjectCommand;
import com.hardcoders.taskmanager.application.command.ReplanProjectCommand;
import com.hardcoders.taskmanager.application.command.UpdateProjectDetailsCommand;
import com.hardcoders.taskmanager.application.result.ProjectResult;

/** Puerto de entrada de la Épica 1 (Jefe): ciclo de vida, presupuesto y delegación de proyectos. */
public interface ManageProjectsUseCase {

    /** US1.1 — solo Jefe; crea la revisión 1 del plan en la misma transacción. */
    ProjectResult create(CreateProjectCommand command);

    /** Jefe o Encargado del proyecto: nombre y descripción. */
    ProjectResult updateDetails(UpdateProjectDetailsCommand command);

    /** US1.1 — solo Jefe: nueva revisión de presupuesto/duración. */
    ProjectResult replan(ReplanProjectCommand command);

    /** US1.1 — solo Jefe: ciclo de vida del proyecto. */
    ProjectResult changeStatus(ChangeProjectStatusCommand command);

    /** US1.2 — solo Jefe: delega o reasigna el Encargado. */
    ProjectResult delegate(DelegateProjectCommand command);
}
