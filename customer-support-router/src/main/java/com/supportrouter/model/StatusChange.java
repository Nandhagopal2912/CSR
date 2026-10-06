package com.supportrouter.model;

import java.time.LocalDateTime;

public record StatusChange(String oldStatus, String newStatus, LocalDateTime changedAt) {
}
