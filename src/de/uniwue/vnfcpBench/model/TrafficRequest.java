package de.uniwue.vnfcpBench.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Objects of this class represent traffic demands.
 * They include ingress and egress nodes, required bandwidth,
 * requested maximum delays and an array of requested {@link VNF}s,
 * which are to be applied on a path between ingress and egress..
 *
 * @author alex
 */
public class TrafficRequest implements Comparable<TrafficRequest> {
    /**
     * Source of this demand's traffic.
     */
    public final Node ingress;
    /**
     * Destination of this demand's traffic.
     */
    public final Node egress;
    /**
     * Required bandwidth of this flow. (Mb/s)
     */
    public final double bandwidthDemand;
    /**
     * Maximum allowed latency of this flow's traffic. (μs)
     */
    public final double expectedDelay;
    /**
     * Array of {@link VNF}s which should be applied to this flow.
     */
    public final VNF[] vnfSequence;
    /**
     * Unique ID for equals/hashCode (in case of 2 distinct requests with equal ingress, egress, ...)
     */
    public final int id;

    /**
     * Erzeugt ein neues TrafficRequest-Objekt.
     *
     * @param id              Unique ID for equals/hashCode (in case of 2 distinct requests with equal ingress, egress, ...)
     * @param ingress         Source of this demand's traffic.
     * @param egress          Destination of this demand's traffic.
     * @param bandwidthDemand Required bandwidth of this flow. (Mb/s)
     * @param expectedDelay   Maximum allowed latency of this flow's traffic. (μs)
     * @param vnfSequence     Array of {@link VNF}s which should be applied to this flow.
     */
    public TrafficRequest(int id, Node ingress, Node egress, double bandwidthDemand, double expectedDelay, VNF[] vnfSequence) {
        this.id = id;
        this.ingress = Objects.requireNonNull(ingress);
        this.egress = Objects.requireNonNull(egress);
        this.bandwidthDemand = bandwidthDemand;
        this.expectedDelay = expectedDelay;
        this.vnfSequence = Objects.requireNonNull(vnfSequence);

        // Sanity-Check:
        for (VNF vnf : vnfSequence) {
            if (vnf == null) {
                throw new NullPointerException("vnfSequence contains null");
            }
            if (vnf.processingCapacity < bandwidthDemand) {
                throw new IllegalArgumentException("processingCapacity of " + vnf + " too small for bandwidth " + bandwidthDemand);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrafficRequest that = (TrafficRequest) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "TrafficRequest{" +
                "ingress=" + ingress +
                ", egress=" + egress +
                ", bandwidthDemand=" + bandwidthDemand +
                ", expectedDelay=" + expectedDelay +
                ", vnfSequence=" + Arrays.toString(vnfSequence) +
                '}';
    }

    /**
     * Useful for exporting a request..
     *
     * @return CSV-Representation of this request.
     */
    public String toCsv() {
        String ret = ingress.name+","+egress.name+","+ NetworkGraph.noDigits(bandwidthDemand * 1000.0)+","+ NetworkGraph.noDigits(expectedDelay)+",";
        ret += Arrays.stream(vnfSequence).map(v -> v.name.toLowerCase()).collect(Collectors.joining(","));
        return ret;
    }

    @Override
    public int compareTo(TrafficRequest o) {
        return Double.compare(bandwidthDemand, o.bandwidthDemand);
    }
}
