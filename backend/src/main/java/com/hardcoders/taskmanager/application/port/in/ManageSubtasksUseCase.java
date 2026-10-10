package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.command.CreateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.command.UpdateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.result.SubtaskResult;

/** Puerto de entrada: comandos sobre subtareas. Cada operación es atómica. */
public interface ManageSubtasksUseCase {

    /** US2.2: da de alta una subtarea de una tarea del proyecto que gestiona el actor. */
    SubtaskResult create(CreateSubtaskCommand command);

    SubtaskResult update(UpdateSubtaskCommand command);

    /** US3.2 / US1.3: solo el Personal asignado a la tarea, o el Jefe como excepción autorizada. */
    SubtaskResult start(TransitionCommand command);

    SubtaskResult pause(TransitionCommand command);

    SubtaskResult complete(TransitionCommand command);

    SubtaskResult reopen(TransitionCommand command);

    void cancel(VersionedCommand command);
}
