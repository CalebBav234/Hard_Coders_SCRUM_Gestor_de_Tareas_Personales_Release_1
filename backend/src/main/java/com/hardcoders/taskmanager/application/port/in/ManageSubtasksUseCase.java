package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.command.CreateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.UpdateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.result.SubtaskResult;

/** Puerto de entrada: comandos sobre subtareas. Cada operación es atómica. */
public interface ManageSubtasksUseCase {

    SubtaskResult create(CreateSubtaskCommand command);

    SubtaskResult update(UpdateSubtaskCommand command);

    SubtaskResult start(VersionedCommand command);

    SubtaskResult pause(VersionedCommand command);

    SubtaskResult complete(VersionedCommand command);

    SubtaskResult reopen(VersionedCommand command);

    void cancel(VersionedCommand command);
}
