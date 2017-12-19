package de.uniwue.vnfcpBench.model;

import de.uniwue.vnfcpBench.model.solution.ParetoFrontier;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class ProblemInstance {
    public final NetworkGraph ng;
    public final VnfLib vnfLib;
    public final TrafficRequest[] reqs;
    public ParetoFrontier solution;

    public ProblemInstance(NetworkGraph ng, VnfLib vnfLib, TrafficRequest[] reqs) {
        this.ng = ng;
        this.vnfLib = vnfLib;
        this.reqs = reqs;
    }
}
