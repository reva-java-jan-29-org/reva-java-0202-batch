package com.training.campusaccess.services.authenticators;

import com.training.campussystem.model.enums.AccessRequest;
import com.training.campussystem.model.enums.AuthType;

public final class RfidAuthenticator implements Authenticator {

	@Override
	public AuthType supportedType() {
		// TODO Auto-generated method stub
		return AuthType.RFID;
	}

	@Override
	public boolean authenticate(AccessRequest request) {
		String expected = "RFID:" + request.getPerson().getId();
        return request.getCredentialData().trim().equalsIgnoreCase(expected);
	}

}
