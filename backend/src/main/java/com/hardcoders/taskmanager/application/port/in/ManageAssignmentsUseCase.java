package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.command.AssignmentCommands.AssignWorkerCommand;
import com.hardcoders.taskmanager.application.command.AssignmentCommands.UnassignWorkerCommand;
import com.hardcoders.taskmanager.application.result.TaskAssignmentResult;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;

import java.util.List;

/** Puerto de entrada de US2.3: asignación y revocación de Personal en tareas. */
public interface ManageAssignmentsUseCase {

    TaskAssignmentResult assign(AssignWorkerCommand command);

    void unassign(UnassignWorkerCommand command);

    /** Asignaciones vigentes de la tarea (Jefe o Encargado del proyecto). */
    List<TaskAssignmentResult> assignmentsForTask(Integer taskId);

    /** Usuarios activos asignables en el proyecto (Jefe o Encargado del proyecto). */
    List<UserSummaryResult> assignableWorkers(Integer projectId);
}
