package com.hardcoders.taskmanager.application.command;

public record CreateSubtaskCommand(Integer taskId, String title, String description) {
}
