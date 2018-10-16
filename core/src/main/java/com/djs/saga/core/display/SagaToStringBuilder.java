package com.djs.saga.core.display;

import java.util.stream.IntStream;

import com.djs.saga.core.saga.SagaId;

import lombok.RequiredArgsConstructor;

public class SagaToStringBuilder implements ToStringPart {

	private final int indent;
	private final int inputPad;
	private final ToStringPart parent;
	private final ToStringPart self;

	private String fqCache;

	public SagaToStringBuilder(int indent, int inputPad, ToStringPart parent, ToStringPart self) {
		this.indent = indent;
		this.inputPad = inputPad;
		this.parent = parent;
		this.self = self;
	}

	public static SagaToStringBuilder start(int indent) {
		return new SagaToStringBuilder(indent, 0, null, null){
			@Override
			public void append(StringBuilder sb, int inputPad) {
			}
		};
	}

	private SagaToStringBuilder append(ToStringPart toStringPart) {
		return new SagaToStringBuilder(
				indent,
				Math.max(toStringPart.inputSize(), inputPad),
				this,
				toStringPart
		);
	}

	public SagaToStringBuilder appendSaga(SagaId sagaId, Object input) {
		return append(new Saga(input, sagaId));
	}

	public SagaToStringBuilder appendStep(String stepName, Object input) {
		return append(new Step(input, stepName));
	}

	public SagaToStringBuilder appendBranch(String branchName) {
		return append(new Branch(branchName));
	}

	public void append(StringBuilder sb, int inputPad) {
		if (this.inputPad != inputPad) {
			parent.append(sb, inputPad);
			if(sb.length() != 0){
				sb.append(System.lineSeparator());
			}
			IntStream.range(0, indent).forEach(i -> sb.append(" "));
			self.append(sb, inputPad);
		} else if (fqCache == null) {
			parent.append(sb, inputPad);
			if(sb.length() != 0){
				sb.append(System.lineSeparator());
			}
			IntStream.range(0, indent).forEach(i -> sb.append(" "));
			self.append(sb, inputPad);
			fqCache = sb.toString();
		} else {
			sb.append(fqCache);
		}
	}

	public String build() {
		if (fqCache == null){
			append(new StringBuilder(), inputPad);
		}
		return fqCache;
	}

	@Override
	public int inputSize() {
		return inputPad;
	}

	private static class Saga implements ToStringPart {

		private final String input;
		private final String sagaId;

		public Saga(Object input, SagaId sagaId) {
			String edge = input instanceof String ? "\"" : "";
			this.input = String.format("%s%s%s", edge, input, edge);
			this.sagaId = String.format("%s:%s", sagaId.getName(), sagaId.getVersion());
		}

		public void append(StringBuilder sb, int inputPad) {
			sb.append("INPUT[ ").append(input).append(" ] ");
			IntStream.range(0, Math.max(0, inputPad - inputSize())).forEach(i -> sb.append("-"));
			sb.append("-> SAGA[ ").append(sagaId).append(" ]");
		}

		@Override
		public int inputSize() {
			return input.length();
		}

	}

	@RequiredArgsConstructor
	private static class Step implements ToStringPart {

		private final String input;
		private final String stepName;

		public Step(Object input, String stepName) {
			String edge = input instanceof String ? "\"" : "";
			this.input = String.format("%s%s%s", edge, input, edge);
			this.stepName = stepName;
		}

		public void append(StringBuilder sb, int inputPad) {
			sb.append("INPUT[ ").append(input).append(" ] ");
			IntStream.range(0, Math.max(0, inputPad - inputSize())).forEach(i -> sb.append("-"));
			sb.append("-> STEP[ ").append(stepName).append(" ]");
		}

		@Override
		public int inputSize() {
			return input.length();
		}

	}

	private static class Branch implements ToStringPart {

		private final String branchName;

		public Branch(String branchName) {
			this.branchName = branchName;
		}

		public void append(StringBuilder sb, int inputPad) {
			IntStream.range(0, inputPad + 10).forEach(i -> sb.append(" "));
			sb.append("-> BRANCH[ ").append(branchName).append(" ]");
		}

		@Override
		public int inputSize() {
			return 0;
		}

	}


}
