package de.uniwue.vnfcpBench.generators.chains;

import de.uniwue.vnfcpBench.model.VNF;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class FixedChainGenerator implements ChainGenerator {
	private ArrayList<VNF[]> chains;
	private Random rand;

	public FixedChainGenerator(Random rand) {
		chains = new ArrayList<>();
		this.rand = Objects.requireNonNull(rand);
	}

	public void addChain(VNF[] chain) {
		chains.add(Objects.requireNonNull(chain));
	}

	@Override
	public VNF[] generate() {
		return chains.get(rand.nextInt(chains.size()));
	}
}
