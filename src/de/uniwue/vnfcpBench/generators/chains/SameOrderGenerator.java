package de.uniwue.vnfcpBench.generators.chains;

import de.uniwue.vnfcpBench.model.VNF;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

public class SameOrderGenerator implements ChainGenerator {
	private VNF[] order;
	private int[] chainLength;
	private Random rand;

	public SameOrderGenerator(VNF[] order, int[] chainLength, Random rand) {
		this.order = Objects.requireNonNull(order);
		this.chainLength = Objects.requireNonNull(chainLength);
		this.rand = Objects.requireNonNull(rand);

		if (chainLength.length != 2) {
			throw new IllegalArgumentException("chainLength.length = " + chainLength.length);
		}
		if (chainLength[0] < 1) {
			throw new IllegalArgumentException("chainLength[0] = " + chainLength[0]);
		}
		if (chainLength[0] > chainLength[1]) {
			throw new IllegalArgumentException("chainLength[0] > chainLength[1]: " + chainLength[0] + " > " + chainLength[1]);
		}
	}

	@Override
	public VNF[] generate() {
		int length = chainLength[0] + rand.nextInt(chainLength[1] - chainLength[0] + 1);

		int[] remaining = IntStream.range(0, order.length).toArray();
		int[] removed = Arrays.copyOf(remaining, remaining.length);
		Collections.shuffle(Arrays.asList(removed), rand);
		for (int i = 0; i < order.length - length; i++) {
			remaining[removed[i]] = -1;
		}

		VNF[] result = new VNF[length];
		int index = 0;
		for (int i = 0; i < order.length; i++) {
			if (remaining[i] != -1) {
				result[index] = order[remaining[i]];
				index++;
			}
		}
		return result;
	}
}
