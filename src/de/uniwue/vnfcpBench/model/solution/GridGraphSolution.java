package de.uniwue.vnfcpBench.model.solution;

import de.uniwue.vnfcpBench.model.Node;

import java.util.Arrays;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class GridGraphSolution implements Solution {
    public final Node[][] vnfSeqs;
    public final double cpu;
    public final double hops;
    private int usedNodes = -1;

    public GridGraphSolution(Node[][] vnfSeqs, double cpu, double hops) {
        if (vnfSeqs != null) {
            this.vnfSeqs = new Node[vnfSeqs.length][];
            for (int i = 0; i < vnfSeqs.length; i++) {
                this.vnfSeqs[i] = Arrays.copyOf(vnfSeqs[i], vnfSeqs[i].length);
            }
        }
        else {
            this.vnfSeqs = new Node[0][];
        }
        this.cpu = cpu;
        this.hops = hops;
    }

    @Override
    public double[] getObj() {
        return new double[]{cpu, hops};
    }

    @Override
    public int getInstances() {
        return getUsedNodes();
    }

    @Override
    public int getUsedNodes() {
        if (usedNodes == -1) {
            usedNodes = (int) Arrays.stream(vnfSeqs)
                    .flatMap(Arrays::stream)
                    .distinct()
                    .count();
        }
        return usedNodes;
    }
}
