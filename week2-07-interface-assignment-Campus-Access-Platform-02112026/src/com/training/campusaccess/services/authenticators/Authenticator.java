package com.training.campusaccess.services.authenticators;

import com.training.campussystem.model.enums.AccessRequest;
import com.training.campussystem.model.enums.AuthType;

public interface Authenticator {

	AuthType supportedType();
    boolean authenticate(AccessRequest request);
}
