package de.uniwue.vnfcpBench.model.solution;

import de.uniwue.vnfcpBench.model.Node;
import de.uniwue.vnfcpBench.model.VNF;

import java.util.Objects;

public class VnfInstance {
	public final Node node;
	public final VNF vnf;
	public double usedCapacity;

	public VnfInstance(Node node, VNF vnf) {
		this.node = Objects.requireNonNull(node);
		this.vnf = Objects.requireNonNull(vnf);
		this.usedCapacity = 0.0;
	}

	public double getRemainingCapacity() {
		return vnf.processingCapacity - usedCapacity;
	}
}
