package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.command.ChangeActivityCommand;
import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.UpdateTaskPlanCommand;
import com.hardcoders.taskmanager.application.command.UpdateTaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.result.TaskResult;

/** Puerto de entrada: comandos sobre tareas. Cada operación es atómica. */
public interface ManageTasksUseCase {

    TaskResult create(CreateTaskCommand command);

    TaskResult update(UpdateTaskCommand command);

    TaskResult changeActivity(ChangeActivityCommand command);

    /** US2.2: presupuesto asignado y duración estimada, con revisión en {@code task_plan_history}. */
    TaskResult updatePlan(UpdateTaskPlanCommand command);

    /** US3.2 / US1.3: el Personal asignado o el Jefe; el motivo del Jefe queda en el historial. */
    TaskResult start(TransitionCommand command);

    TaskResult pause(TransitionCommand command);

    TaskResult complete(TransitionCommand command);

    TaskResult reopen(TransitionCommand command);

    void cancel(VersionedCommand command);
}
