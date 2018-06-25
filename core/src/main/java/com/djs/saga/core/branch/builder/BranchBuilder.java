package com.djs.saga.core.branch.builder;

public class BranchBuilder {

	public BranchBuilderAwait start(String branchName) {
		return new BranchBuilderAwait(branchName);
	}

}
