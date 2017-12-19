package de.uniwue.vnfcpBench.model.solution;

import de.uniwue.vnfcpBench.model.Node;

import java.util.Arrays;
import java.util.Objects;

public class TrafficFlow {
	public final Node src;
	public final Node dst;
	public final Hop[] path;
	public final VnfInstance[] instances;

	private double delay = -1.0;
	private int hops = -1;

	public TrafficFlow(Node src, Node dst, Hop[] path) {
		this.src = Objects.requireNonNull(src);
		this.dst = Objects.requireNonNull(dst);
		this.path = Objects.requireNonNull(path);
		this.instances = Arrays.stream(path).map(h -> h.inst).filter(Objects::nonNull).toArray(VnfInstance[]::new);
	}

	public double getDelay() {
		if (delay == -1.0) {
			delay = Arrays.stream(path).filter(p -> p.previous != null).mapToDouble(p -> p.previous.delay).sum();
			delay += Arrays.stream(instances).mapToDouble(i -> i.vnf.delay).sum();
		}
		return delay;
	}

	public int getHops() {
		if (hops == -1) {
			hops = (int) Arrays.stream(path).filter(p -> p.previous != null).count();
		}
		return hops;
	}
}
