package com.hardcoders.taskmanager.application.command;

public record DelegateProjectCommand(Integer projectId, Integer managerUserId, String reason, Long version) {
}
