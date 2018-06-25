package com.djs.saga.core.branches;

import java.util.UUID;

public interface Branches {

	BranchesOutput await(String parentName, UUID correlationId);

}
