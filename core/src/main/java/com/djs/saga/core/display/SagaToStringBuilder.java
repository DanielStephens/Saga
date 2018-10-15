package com.djs.saga.core.display;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import com.djs.saga.core.saga.SagaId;
import com.google.common.collect.ImmutableList;

import lombok.RequiredArgsConstructor;

public class SagaToStringBuilder {

	private final int indent;
	private final int inputTrim;
	private final int definitionTrim;
	private final List<ToString> lines;
	private String cache;

	public SagaToStringBuilder(int indent, int inputTrim, int definitionTrim, List<ToString> lines) {
		this.indent = indent;
		this.inputTrim = inputTrim;
		this.definitionTrim = definitionTrim;
		this.lines = lines;
	}

	public static SagaToStringBuilder start(int indent, int inputTrim, int definitionTrim) {
		return new SagaToStringBuilder(indent, inputTrim, definitionTrim, Collections.emptyList());
	}

	private static String cut(Object o, int size) {
		String str = o.toString();
		str = str.substring(0, Math.min(str.length(), size));
		return str;
	}

	private SagaToStringBuilder append(ToString toString) {
		return new SagaToStringBuilder(
				indent,
				inputTrim,
				definitionTrim,
				ImmutableList.<ToString>builder()
						.addAll(lines)
						.add(toString)
						.build()
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

	public String build() {
		if (cache != null) {
			return cache;
		}

		StringBuilder sb = new StringBuilder(lines.size() * 20);

		lines.forEach(s -> s.append(sb, indent, Math.max(inputTrim, 5), Math.max(definitionTrim, 5)));

		cache = sb.toString();
		return cache;
	}

	private interface ToString {

		void append(StringBuilder sb, int indent, int inputTrim, int definitionTrim);

	}

	@RequiredArgsConstructor
	private static class Saga implements ToString {

		private final Object value;
		private final SagaId sagaId;
		private String cache;


		public void append(StringBuilder sb, int indent, int inputTrim, int definitionTrim) {
			if (cache != null) {
				sb.append(cache);
				return;
			}

			StringBuilder sb2 = new StringBuilder(indent + inputTrim + definitionTrim + 12);

			String o = cut(value, inputTrim);
			int diff = inputTrim - o.length();
			String v = String.valueOf(sagaId.getVersion());
			String n = cut(sagaId.getName(), definitionTrim - (v.length() + 1));

			IntStream.range(0, indent).forEach(i -> sb2.append(" "));
			sb2.append("INPUT[ ").append(o).append(" ]");
			IntStream.range(0, diff).forEach(i -> sb2.append("-"));
			sb2.append("--> SAGA[ ").append(n).append(" ]");

			cache = sb2.toString();
			sb.append(cache);
		}

	}

	@RequiredArgsConstructor
	private static class Step implements ToString {

		private final Object value;
		private final String stepName;
		private String cache;


		public void append(StringBuilder sb, int indent, int inputTrim, int definitionTrim) {
			if (cache != null) {
				sb.append(cache);
				return;
			}

			StringBuilder sb2 = new StringBuilder(indent + inputTrim + definitionTrim + 12);

			String o = cut(value, inputTrim);
			int diff = inputTrim - o.length();
			String n = cut(stepName, definitionTrim);

			sb2.append(System.lineSeparator());
			IntStream.range(0, indent).forEach(i -> sb2.append(" "));
			sb2.append("INPUT[ ").append(o).append(" ]");
			IntStream.range(0, diff).forEach(i -> sb2.append("-"));
			sb2.append("--> STEP[ ").append(n).append(" ]");

			cache = sb2.toString();
			sb.append(cache);
		}

	}

	@RequiredArgsConstructor
	private static class Branch implements ToString {

		private final String branchName;
		private String cache;


		public void append(StringBuilder sb, int indent, int inputTrim, int definitionTrim) {
			if (cache != null) {
				sb.append(cache);
				return;
			}

			StringBuilder sb2 = new StringBuilder(indent + inputTrim + definitionTrim + 14);

			String n = cut(branchName, definitionTrim);

			sb2.append(System.lineSeparator());
			IntStream.range(0, indent + inputTrim + 13).forEach(i -> sb2.append(" "));
			sb2.append("BRANCH[ ").append(n).append(" ]");

			cache = sb2.toString();
			sb.append(cache);
		}

	}


}
