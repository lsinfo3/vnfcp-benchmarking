package de.uniwue.vnfcpBench.model.solution;

import de.uniwue.vnfcpBench.model.Link;
import de.uniwue.vnfcpBench.model.Node;

import java.util.Objects;

public class Hop {
	public final Node currentNode;
	public final Link previous;
	public final VnfInstance inst;

	public Hop(Node currentNode, Link previous, VnfInstance inst) {
		this.currentNode = Objects.requireNonNull(currentNode);
		this.previous = previous;
		this.inst = inst;
	}
}
