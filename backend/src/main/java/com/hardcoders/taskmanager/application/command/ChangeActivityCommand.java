package com.hardcoders.taskmanager.application.command;

public record ChangeActivityCommand(Integer taskId, String activityName, Long version) {
}
