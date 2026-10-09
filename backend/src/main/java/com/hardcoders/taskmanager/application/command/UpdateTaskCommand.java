package com.hardcoders.taskmanager.application.command;

public record UpdateTaskCommand(Integer taskId, String title, String description, String priority, Long version) {
}
