package com.training.campusaccess.services.policies;

import com.training.campussystem.model.enums.AccessRequest;
import com.training.campussystem.model.enums.AccessResult;
import com.training.campussystem.model.enums.Zone;

public interface AccessPolicy {

	 boolean supports(Zone zone);
     AccessResult evaluate(AccessRequest request);

}
