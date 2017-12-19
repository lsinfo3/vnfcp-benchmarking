package de.uniwue.vnfcpBench.generators.chains;

import de.uniwue.vnfcpBench.model.VNF;
import de.uniwue.vnfcpBench.model.VnfLib;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;

public class RandomOrderGenerator implements ChainGenerator {
	private VnfLib lib;
	private int[] chainLength;
	private Random rand;

	public RandomOrderGenerator(VnfLib lib, int[] chainLength, Random rand) {
		this.lib = Objects.requireNonNull(lib);
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
		VNF[] vnfs = lib.getAllVnfs().toArray(new VNF[lib.size]);
		Collections.shuffle(Arrays.asList(vnfs), rand);
		return Arrays.copyOf(vnfs, length);
	}
}
