package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.command.HierarchyCommands.ActivityStatusCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateActivityCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateStageCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.StageStatusCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.UpdateActivityCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.UpdateStageCommand;
import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.StageResult;

/** Puerto de entrada de la Épica 2: construcción de la jerarquía Proyecto → Etapa → Actividad. */
public interface ManageHierarchyUseCase {

    StageResult createStage(CreateStageCommand command);

    StageResult updateStage(UpdateStageCommand command);

    StageResult changeStageStatus(StageStatusCommand command);

    ActivityResult createActivity(CreateActivityCommand command);

    ActivityResult updateActivity(UpdateActivityCommand command);

    ActivityResult changeActivityStatus(ActivityStatusCommand command);
}
