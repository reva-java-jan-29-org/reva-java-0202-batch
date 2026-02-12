package com.training.campussystem.model.enums;

import java.time.LocalDateTime;
import java.util.Objects;

public final class AccessResult {
	private final boolean allowed;
	private final String reason;
	private final LocalDateTime timestamp;

	private AccessResult(boolean allowed, String reason, LocalDateTime timestamp) {
		this.allowed = allowed;
		this.reason = Objects.requireNonNull(reason);
		this.timestamp = Objects.requireNonNull(timestamp);
	}

	public static AccessResult allow(String reason, LocalDateTime timestamp) {
		return new AccessResult(true, reason, timestamp);
	}

	public static AccessResult deny(String reason, LocalDateTime timestamp) {
		return new AccessResult(false, reason, timestamp);
	}

	public boolean isAllowed() {
		return allowed;
	}

	public String getReason() {
		return reason;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}
}
