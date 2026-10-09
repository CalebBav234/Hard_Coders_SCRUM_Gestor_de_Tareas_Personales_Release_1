package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.command.ChangeActivityCommand;
import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.command.UpdateTaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.result.TaskResult;

/** Puerto de entrada: comandos sobre tareas. Cada operación es atómica. */
public interface ManageTasksUseCase {

    TaskResult create(CreateTaskCommand command);

    TaskResult update(UpdateTaskCommand command);

    TaskResult changeActivity(ChangeActivityCommand command);

    TaskResult start(VersionedCommand command);

    TaskResult pause(VersionedCommand command);

    TaskResult complete(VersionedCommand command);

    TaskResult reopen(VersionedCommand command);

    void cancel(VersionedCommand command);
}
