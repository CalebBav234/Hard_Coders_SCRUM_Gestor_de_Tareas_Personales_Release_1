package com.hardcoders.taskmanager.application.command;

public record ChangeProjectStatusCommand(Integer projectId, String status, Long version) {
}
