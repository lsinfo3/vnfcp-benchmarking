package de.uniwue.vnfcpBench.model.solution;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

public class DynamicDistSolution implements Solution {
	public final TrafficAssignment[] assigs;
	private double[] obj;
	private int instances = -1;
	private int usedNodes = -1;

	public DynamicDistSolution(TrafficAssignment[] assigs) {
		this.assigs = Objects.requireNonNull(assigs);
	}

	@Override
	public double[] getObj() {
		if (obj == null) {
			HashSet<VnfInstance> instances = Arrays.stream(assigs)
					.flatMap(a -> Arrays.stream(a.flow.instances))
					.collect(Collectors.toCollection(HashSet::new));
			double cpu = instances.stream().mapToDouble(i -> i.vnf.cpuRequired).sum();
			obj = new double[]{cpu};
		}
		return Arrays.copyOf(obj, obj.length);
	}

	@Override
	public int getInstances() {
		if (instances == -1) {
			instances = (int) Arrays.stream(assigs)
					.flatMap(a -> Arrays.stream(a.flow.instances))
					.distinct()
					.count();
		}
		return instances;
	}

	@Override
	public int getUsedNodes() {
		if (usedNodes == -1) {
			usedNodes = (int) Arrays.stream(assigs)
					.flatMap(a -> Arrays.stream(a.flow.instances))
					.map(i -> i.node)
					.distinct()
					.count();
		}
		return usedNodes;
	}
}
