package com.hardcoders.taskmanager.application.command;

public record UpdateProjectDetailsCommand(Integer projectId, String name, String description, Long version) {
}
