package de.uniwue.vnfcpBench.model.solution;

import de.uniwue.vnfcpBench.model.TrafficRequest;

import java.util.Objects;

public class TrafficAssignment {
	public final TrafficRequest req;
	public final TrafficFlow flow;

	public TrafficAssignment(TrafficRequest req, TrafficFlow flow) {
		this.req = Objects.requireNonNull(req);
		this.flow = Objects.requireNonNull(flow);
	}
}
