package com.hardcoders.taskmanager.application.result;

public record UserSummaryResult(Integer userId, String fullName, String email, boolean chief, boolean active) {
}
