package com.training.campussystem.model.enums;

import java.time.LocalDateTime;
import java.util.Objects;

public final class AccessRequest {
	private final Person person;
	private final Zone zone;
	private final LocalDateTime timestamp;
	private final AuthType authType;
	private final String credentialData;

	public AccessRequest(Person person, Zone zone, LocalDateTime timestamp, AuthType authType, String credentialData) {
		this.person = Objects.requireNonNull(person);
		this.zone = Objects.requireNonNull(zone);
		this.timestamp = Objects.requireNonNull(timestamp);
		this.authType = Objects.requireNonNull(authType);
		this.credentialData = Objects.requireNonNull(credentialData);
	}

	public Person getPerson() {
		return person;
	}

	public Zone getZone() {
		return zone;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public AuthType getAuthType() {
		return authType;
	}

	public String getCredentialData() {
		return credentialData;
	}
}
