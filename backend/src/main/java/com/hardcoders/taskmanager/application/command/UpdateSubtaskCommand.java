package com.hardcoders.taskmanager.application.command;

public record UpdateSubtaskCommand(Integer subtaskId, String title, String description, Long version) {
}
