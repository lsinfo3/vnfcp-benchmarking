package de.uniwue.vnfcpBench.generators;

import de.uniwue.vnfcpBench.model.*;
import de.uniwue.vnfcpBench.model.solution.ParetoFrontier;
import de.uniwue.vnfcpBench.model.solution.GridGraphSolution;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class GridGraphProblem implements ProblemGenerator {
    private final int minM;
    private final int maxM;
    private final int minK;
    private final int maxK;
    private final int minN;
    private final int maxN;
    private final double rho;
    private final int vnfsPerNode;
    private final Random rand;

    public GridGraphProblem(int minM, int maxM, int minK, int maxK, int minN, int maxN, double rho, int vnfsPerNode, Random rand) {
        this.minM = minM;
        this.maxM = maxM;
        this.minK = minK;
        this.maxK = maxK;
        this.minN = minN;
        this.maxN = maxN;
        this.rho = rho;
        this.vnfsPerNode = vnfsPerNode;
        this.rand = rand;
    }

    public GridGraphProblem(int minM, int maxM, int minK, int maxK, int minN, int maxN, double rho) {
        this(minM, maxM, minK, maxK, minN, maxN, rho, 1, new Random());
    }

    public GridGraphProblem(int minM, int maxM, int minK, int maxK, int minN, int maxN, double rho, int vnfsPerNode) {
        this(minM, maxM, minK, maxK, minN, maxN, rho, vnfsPerNode, new Random());
    }

    public GridGraphProblem(int minM, int maxM, int minK, int maxK, int minN, int maxN, double rho, Random rand) {
        this(minM, maxM, minK, maxK, minN, maxN, rho, 1, rand);
    }

    @Override
    public GridGraphInstance generate(OutputStream topology, OutputStream vnfLib, OutputStream requests) {
        int m = rand.nextInt(maxM - minM + 1) + minM;
        int k = rand.nextInt(maxK - minK + 1) + minK;
        int n = rand.nextInt(maxN - minN + 1) + minN;
        int n2 = n * vnfsPerNode;

        int d = (int) Math.ceil(m*m * rho);

        // Src and Dst nodes:
        NetworkGraph ng = new NetworkGraph();
        Node[] srcNodes = new Node[m];
        Node[] dstNodes = new Node[m];

        Node[] lastStage = srcNodes;
        for (int i = 0; i < m; i++) {
            lastStage[i] = ng.addNode("src"+i, 0, 0, 0);
            dstNodes[i] = ng.addNode("dst"+i, 0, 0, 0);
        }

        // Links between stages:
        Node[] currentStage = new Node[k];
        Node[] firstStage = currentStage;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < k; j++) {
                currentStage[j] = ng.addNode("n"+i+"k"+j, vnfsPerNode, vnfsPerNode, vnfsPerNode);

                if (i > 0) {
                    ng.addLink(lastStage[j], currentStage[j], (double) d * (n2+1) * 1.2, 1.0);
                }

                if (j > 0) {
                    ng.addLink(currentStage[j-1], currentStage[j], (double) d * (n2+1) * 1.2, 1.0);
                }
            }
            lastStage = currentStage;
            currentStage = new Node[k];
        }

        // Links to src and dst:
        if (m >= k) {
            for (int j = 0; j < m; j++) {
                int nc = Math.max(0, (int) Math.ceil((double) (j + 1) / m * k) - 1);
                int nf = Math.max(0, (int) Math.floor((double) (j + 1) / m * k) - 1);

                ng.addLink(srcNodes[j], firstStage[nc], (double) d * (n2+1) * 1.2, 1.0);
                ng.addLink(lastStage[nc], dstNodes[j], (double) d * (n2+1) * 1.2, 1.0);
                if (nc != nf) {
                    ng.addLink(srcNodes[j], firstStage[nf], (double) d * (n2+1) * 1.2, 1.0);
                    ng.addLink(lastStage[nf], dstNodes[j], (double) d * (n2+1) * 1.2, 1.0);
                }
            }
        }
        else {
            for (int j = 0; j < k; j++) {
                int nc = Math.max(0, (int) Math.ceil((double) (j + 1) / k * m) - 1);
                int nf = Math.max(0, (int) Math.floor((double) (j + 1) / k * m) - 1);

                ng.addLink(srcNodes[nc], firstStage[j], (double) d * (n2+1) * 1.2, 1.0);
                ng.addLink(lastStage[j], dstNodes[nc], (double) d * (n2+1) * 1.2, 1.0);
                if (nc != nf) {
                    ng.addLink(srcNodes[nf], firstStage[j], (double) d * (n2+1) * 1.2, 1.0);
                    ng.addLink(lastStage[j], dstNodes[nf], (double) d * (n2+1) * 1.2, 1.0);
                }
            }
        }

        // Create VNF sequence for all demands:
        VnfLib vLib = new VnfLib();
        VNF[] vnfs = new VNF[n2];
        for (int i = 0; i < n2; i++) {
            vnfs[i] = new VNF("v"+i, 1.0, 1.0, 1.0, 1.0, d * 1.2, -1);
            vLib.addVnf("v"+i, new VNF[]{ vnfs[i] });
        }

        // Shuffle Src and Dst demand pairs:
        Node[] src = new Node[m*m];
        Node[] dst = new Node[m*m];
        for (int i = 0; i < m; i++) {
            System.arraycopy(srcNodes, 0, src, i*m, m);
            System.arraycopy(dstNodes, 0, dst, i*m, m);
        }
        Collections.shuffle(Arrays.asList(src), rand);
        Collections.shuffle(Arrays.asList(dst), rand);

        // Pick d-many demands:
        TrafficRequest[] reqs = new TrafficRequest[d];
        for (int i = 0; i < d; i++) {
            reqs[i] = new TrafficRequest(i, src[i], dst[i], 1.0, ((double) n2+k+1)*(n2+1) * 1.2 + 2.0, vnfs);
        }

        // Export stuff:
        if (topology != null) {
            PrintStream t = new PrintStream(topology);
            t.println(ng.toString());
            t.close();
        }
        if (vnfLib != null) {
            PrintStream v = new PrintStream(vnfLib);
            v.println(vLib.toString());
            v.close();
        }
        if (requests != null) {
            PrintStream r = new PrintStream(requests);
            r.println("# Ingress-ID, Egress-ID, Min-Bandwidth, Max-Delay, VNF, VNF, VNF, ...");
            for (TrafficRequest req : reqs) {
                r.println(req.toCsv());
            }
            r.close();
        }

        return new GridGraphInstance(ng, vLib, reqs, m, k, n);
    }

    public class GridGraphInstance extends ProblemInstance {
        public final int m;
        public final int k;
        public final int n;

        public GridGraphInstance(NetworkGraph ng, VnfLib vnfLib, TrafficRequest[] reqs, int m, int k, int n) {
            super(ng, vnfLib, reqs);
            this.m = m;
            this.k = k;
            this.n = n;
        }
    }

    @Override
    public ParetoFrontier<GridGraphSolution> getSolutions(ProblemInstance inst) {
        if (!(inst instanceof GridGraphInstance)) {
            throw new IllegalArgumentException("inst must be of type GridGraphInstance");
        }
        GridGraphInstance msi = (GridGraphInstance) inst;

        ParetoFrontier<GridGraphSolution> pf = new ParetoFrontier<>();
        int[] placeVnf = new int[msi.k];
        getSolutionsRec(msi, pf, placeVnf, 0);
        return pf;
    }

    public void getSolutionsRec(GridGraphInstance msi, ParetoFrontier<GridGraphSolution> pf, int[] placeVnf, int ki) {
        if (ki >= msi.k) {
            int sum = Arrays.stream(placeVnf).sum();
            if (sum == 0) return;

            VNF randomVNF = msi.vnfLib.getAllVnfs().iterator().next();
            double cpuPerVnf = randomVNF.cpuRequired;
            double cpuRequired = cpuPerVnf * msi.n * sum;

            double numOfHops = msi.reqs.length * (msi.n + 1);
            for (int i = 0; i < msi.reqs.length; i++) {
                TrafficRequest r = msi.reqs[i];

                // Find the shortest path for ingress -> VNF rail -> egress
                int smallestDist = msi.k+1;
                for (Link l : r.ingress.getNeighbours()) {
                    Node n = l.getOther(r.ingress);
                    int inIndex = Integer.parseInt(n.name.split("k")[1]);

                    for (Link l2 : r.egress.getNeighbours()) {
                        Node n2 = l2.getOther(r.egress);
                        int egIndex = Integer.parseInt(n2.name.split("k")[1]);

                        for (int k = 0; k < msi.k; k++) {
                            int dist = Math.abs(inIndex - k) + Math.abs(egIndex - k);
                            if (placeVnf[k] == 1 && (smallestDist == -1 || dist < smallestDist)) {
                                smallestDist = dist;
                            }
                        }
                    }
                }
                numOfHops += smallestDist;
            }

            pf.updateParetoFrontier(new GridGraphSolution(null, cpuRequired, numOfHops));
        }
        else {
            placeVnf[ki] = 1;
            getSolutionsRec(msi, pf, placeVnf, ki+1);
            placeVnf[ki] = 0;
            getSolutionsRec(msi, pf, placeVnf, ki+1);

        }
    }
}
