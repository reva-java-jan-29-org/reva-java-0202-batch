package com.training.campusaccess.services.authenticators;

import com.training.campussystem.model.enums.AccessRequest;
import com.training.campussystem.model.enums.AuthType;

public final class BiometricAuthenticator implements Authenticator {
	@Override
	public AuthType supportedType() {
		return AuthType.BIOMETRIC;
	}

	@Override
	public boolean authenticate(AccessRequest request) {
		String expected = "BIO:" + request.getPerson().getId() + ":OK";
		return request.getCredentialData().trim().equalsIgnoreCase(expected);
	}
}
