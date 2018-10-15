package com.djs.saga.core.branch;

public interface Branch {

	BranchOutput await(BranchParams branchParams);

}
