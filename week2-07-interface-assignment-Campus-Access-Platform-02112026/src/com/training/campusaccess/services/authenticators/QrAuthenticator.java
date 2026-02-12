package com.training.campusaccess.services.authenticators;

import com.training.campussystem.model.enums.AccessRequest;
import com.training.campussystem.model.enums.AuthType;

public final class QrAuthenticator implements Authenticator {
	
	@Override
	public AuthType supportedType() {
		return AuthType.QR;
	}

	@Override
	public boolean authenticate(AccessRequest request) {
		String expected = "QR:" + request.getPerson().getId();
		return request.getCredentialData().trim().equalsIgnoreCase(expected);
	}
}
