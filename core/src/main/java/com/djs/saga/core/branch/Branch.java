package com.djs.saga.core.branch;

import java.util.UUID;

public interface Branch {

	BranchOutput await(String parentName, UUID correlationId);

}
