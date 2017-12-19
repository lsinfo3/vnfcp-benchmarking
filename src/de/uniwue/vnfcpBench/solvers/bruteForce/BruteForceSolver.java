package de.uniwue.vnfcpBench.solvers.bruteForce;

import de.uniwue.vnfcpBench.model.*;
import de.uniwue.vnfcpBench.model.factory.TopologyFileReader;
import de.uniwue.vnfcpBench.model.factory.TrafficRequestsReader;
import de.uniwue.vnfcpBench.model.factory.VnfLibReader;
import de.uniwue.vnfcpBench.model.solution.ParetoFrontier;
import de.uniwue.vnfcpBench.model.solution.GridGraphSolution;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class BruteForceSolver {
    private ProblemInstance inst;
    private ParetoFrontier<GridGraphSolution> pf;
    private VNF[] requestedTypes;
    private Node[] cpuLocations;
    private HashMap<VNF, Integer> vnfToId;
    private HashMap<Node, Integer> nodeToId;
    private ArrayList<Double>[][] usedCapacities;
    private HashMap<Link, Double> usedBandwidths;
    private double solved, all, lastPercent;
    private int[][] requiredInstances;
    private ArrayList<Double> bins;

    public BruteForceSolver(ProblemInstance inst) {
        this.inst = inst;

        // Which instance types are requested?
        requestedTypes = Arrays.stream(inst.reqs).flatMap(r -> Arrays.stream(r.vnfSequence)).distinct().toArray(VNF[]::new);
        vnfToId = new HashMap<>();
        for (int i = 0; i < requestedTypes.length; i++) {
            vnfToId.put(requestedTypes[i], i);
        }

        // Where are computational resources?
        cpuLocations = inst.ng.getNodes().values().stream().filter(n -> n.cpuCapacity > 0.0).distinct().toArray(Node[]::new);
        nodeToId = new HashMap<>();
        for (int i = 0; i < cpuLocations.length; i++) {
            nodeToId.put(cpuLocations[i], i);
        }

        // Prepare data structures globally, so less work for the GC:
        usedBandwidths = new HashMap<>();
        usedCapacities = new ArrayList[requestedTypes.length][cpuLocations.length];
        for (int v = 0; v < requestedTypes.length; v++) {
            for (int n = 0; n < cpuLocations.length; n++) {
                usedCapacities[v][n] = new ArrayList<>();
            }
        }
        requiredInstances = new int[requestedTypes.length][cpuLocations.length];
        bins = new ArrayList<>();
    }

    public ParetoFrontier<GridGraphSolution> solve() {
        pf = new ParetoFrontier<>();
        solved = 0.0;
        all = Math.pow(Math.pow(cpuLocations.length, inst.reqs[0].vnfSequence.length), inst.reqs.length);
        lastPercent = -0.1;

        // Prepare VNF Sequence Data Structure:
        Node[][] vnfSeqs = new Node[inst.reqs.length][];
        for (int i = 0; i < inst.reqs.length; i++) {
            vnfSeqs[i] = new Node[inst.reqs[i].vnfSequence.length];
        }

        // Start instance recursion:
        recursionPaths(0, 0, vnfSeqs);

        return pf;
    }

    public void recursionPaths(int currentRequest, int currentVnf, Node[][] vnfSeqs) {
        if (currentRequest >= inst.reqs.length) {
            // Evaluate current solution:
            evalSolution(vnfSeqs);
        }
        else {
            if (currentVnf >= inst.reqs[currentRequest].vnfSequence.length) {
                recursionPaths(currentRequest+1, 0, vnfSeqs);
            }
            else {
                for (Node n : cpuLocations) {
                    vnfSeqs[currentRequest][currentVnf] = n;
                    recursionPaths(currentRequest, currentVnf+1, vnfSeqs);
                }
            }
        }
    }

    public void evalSolution(Node[][] vnfSeqs) {
        solved++;
        double percent = solved / all * 100.0;
        if (percent - lastPercent >= 0.01) {
            System.out.println(String.format("[%s] Tested %.2f%% of solutions.",
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    percent));
            lastPercent = percent;
        }

        for (Link l : inst.ng.getLinks()) {
            usedBandwidths.put(l, 0.0);
        }

        for (int v = 0; v < requestedTypes.length; v++) {
            for (int n = 0; n < cpuLocations.length; n++) {
                usedCapacities[v][n].clear();
            }
        }

        double totalNumberOfHops = 0.0;

        for (int i = 0; i < inst.reqs.length; i++) {
            TrafficRequest r = inst.reqs[i];
            Node[] seq = vnfSeqs[i];

            // Check delay
            double d = getDelayForRequest(r, seq);
            if (d > r.expectedDelay) return;

            double h = getHopsForRequest(r, seq);
            totalNumberOfHops += h;

            if (!addBandwidthToLinks(r, seq)) return;
            addCapacityToNodes(r, seq);
        }

        for (int v = 0; v < requestedTypes.length; v++) {
            VNF vnf = requestedTypes[v];
            for (int n = 0; n < cpuLocations.length; n++) {
                ArrayList<Double> requiredCap = usedCapacities[v][n];
                double sumCap = 0.0;
                for (Double d : requiredCap) sumCap += d;

                if (!requiredCap.isEmpty()) {
                    if (sumCap <= vnf.processingCapacity) {
                        requiredInstances[v][n] = 1;
                    }
                    else {
                        requiredInstances[v][n] = solveBinPacking(requiredCap, vnf.processingCapacity);
                    }
                }
                else {
                    requiredInstances[v][n] = 0;
                }
            }
        }

        double totalCpuRequired = 0.0;
        for (int n = 0; n < cpuLocations.length; n++) {
            int reqCpus = 0;
            for (int v = 0; v < requestedTypes.length; v++) {
                reqCpus += (int) requestedTypes[v].cpuRequired * requiredInstances[v][n];
            }
            if (reqCpus > cpuLocations[n].cpuCapacity) return;

            totalCpuRequired += reqCpus;
        }

        pf.updateParetoFrontier(new GridGraphSolution(vnfSeqs, totalCpuRequired, totalNumberOfHops));
    }

    public int solveBinPacking(ArrayList<Double> elements, double binSize) {
        int pre = firstFitPacking(elements, binSize);

        double sum = 0.0;
        for (int i = 0; i < elements.size(); i++) sum += elements.get(i);
        int lowerEstimate = (int) Math.ceil(sum / binSize);

        if (pre == lowerEstimate) return pre;

        for (int maxBins = lowerEstimate; maxBins < pre; maxBins++) {
            bins.clear();
            bins.ensureCapacity(maxBins);
            if (attemptBinPacking(elements, binSize, bins, maxBins, 0)) {
                return maxBins;
            }
        }
        return pre;
    }

    public boolean attemptBinPacking(ArrayList<Double> elements, double binSize, ArrayList<Double> bins, int maxBins, int index) {
        if (index >= elements.size()) {
            return true;
        }
        else {
            for (int i = 0; i < maxBins; i++) {
                if (bins.get(i) + elements.get(index) <= binSize) {
                    double prev = bins.get(i);
                    bins.set(i, bins.get(i) + elements.get(index));
                    if (attemptBinPacking(elements, binSize, bins, maxBins, index+1)) return true;
                    bins.set(i, prev);
                }
            }
            return false;
        }
    }

    public int firstFitPacking(ArrayList<Double> elements, double binSize) {
        bins.clear();
        // Sort bandwidth demands (desc):
        elements.sort(Comparator.reverseOrder());

        for (Double d : elements) {
            boolean platzGefunden = false;
            int aktuellerBin = 0;
            while (!platzGefunden) {
                // Current bin does not exist? -> Create new one, add request:
                if (aktuellerBin == bins.size()) {
                    bins.add(d);
                    platzGefunden = true;
                }
                // Request fits into current bin:
                else if (bins.get(aktuellerBin) + d <= binSize) {
                    bins.set(aktuellerBin, bins.get(aktuellerBin) + d);
                    platzGefunden = true;
                }
                // Request does not fit, but more bins exist:
                else {
                    aktuellerBin++;
                }
            }
        }

        return bins.size();
    }

    public void addCapacityToNodes(TrafficRequest r, Node[] vnfSeq) {
        for (int i = 0; i < vnfSeq.length; i++) {
            usedCapacities
                    [vnfToId.get(r.vnfSequence[i])]
                    [nodeToId.get(vnfSeq[i])]
                    .add(r.bandwidthDemand);
        }
    }

    public boolean addBandwidthToLinks(TrafficRequest r, Node[] vnfSeq) {
        HashMap<Node, HashMap<Node, Node.Att>> bfs = inst.ng.getBfsBackpointers();

        Node last = r.ingress;
        for (Node n : vnfSeq) {
            HashMap<Node, Node.Att> prevs = bfs.get(last);

            Node current = n;
            Link l = prevs.get(current).pi;
            while (l != null) {
                // Check link's bandwidth
                double bw = usedBandwidths.get(l) + r.bandwidthDemand;
                if (bw > l.bandwidth) {
                    System.out.println("Link (" + l.node1.name + " - " + l.node2.name + ") crowded.");
                    return false;
                }

                usedBandwidths.put(l, bw);
                current = l.getOther(current);
                l = prevs.get(current).pi;
            }

            last = n;
        }
        // Egress:
        HashMap<Node, Node.Att> prevs = bfs.get(last);

        Node current = r.egress;
        Link l = prevs.get(current).pi;
        while (l != null) {
            // Check link's bandwidth
            double bw = usedBandwidths.get(l) + r.bandwidthDemand;
            if (bw > l.bandwidth) {
                System.out.println("Link (" + l.node1.name + " - " + l.node2.name + ") crowded.");
                return false;
            }

            usedBandwidths.put(l, bw);
            current = l.getOther(current);
            l = prevs.get(current).pi;
        }

        return true;
    }

    public double getDelayForRequest(TrafficRequest r, Node[] vnfSeq) {
        HashMap<Node, HashMap<Node, Node.Att>> bfs = inst.ng.getBfsBackpointers();

        double d = 0.0;
        Node last = r.ingress;
        for (Node n : vnfSeq) {
            d += bfs.get(last).get(n).delay;
            last = n;
        }
        d += bfs.get(last).get(r.egress).delay;

        for (VNF v : r.vnfSequence) {
            d += v.delay;
        }

        return d;
    }

    public double getHopsForRequest(TrafficRequest r, Node[] vnfSeq) {
        HashMap<Node, HashMap<Node, Node.Att>> bfs = inst.ng.getBfsBackpointers();

        double h = 0.0;
        Node last = r.ingress;
        for (Node n : vnfSeq) {
            h += bfs.get(last).get(n).h;
            last = n;
        }
        h += bfs.get(last).get(r.egress).h;

        return h;
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        String base = "/home/alex/w/17/benchmark-vnfcp-generator/java/VNFCP_benchmarking/res/eval-topo/";
        NetworkGraph ng = TopologyFileReader.readFromFile(base + "topology");
        VnfLib lib = VnfLibReader.readFromFile(base + "vnfLib2");
        TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(base + "requests", ng, lib);
        ProblemInstance pi = new ProblemInstance(ng, lib, reqs);

        ParetoFrontier<GridGraphSolution> pf = new BruteForceSolver(pi).solve();
        System.out.println("Frontier [CPU, Hops]:");
        for (GridGraphSolution s : pf) {
            System.out.println(Arrays.toString(s.getObj()));
            for (int i = 0; i < reqs.length; i++) {
                System.out.println("  " + reqs[i].ingress.name + " - " + reqs[i].egress.name + ": " + Arrays.stream(s.vnfSeqs[i]).map(n -> n.name).collect(Collectors.joining(",")));
            }
        }
    }

    // Create demands for the problem:
    public static void main2(String[] args) {
        Locale.setDefault(Locale.US);
        String[] hosts = new String[]{"h0", "h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8"};
        String[] srcs = Arrays.copyOf(hosts, hosts.length * hosts.length);
        for (int i = 1; i < hosts.length; i++) {
            System.arraycopy(srcs, 0, srcs, i * hosts.length, hosts.length);
        }
        String[] dsts = Arrays.copyOf(srcs, srcs.length);
        Collections.shuffle(Arrays.asList(srcs));
        Collections.shuffle(Arrays.asList(dsts));
        Random r = new Random();

        // Pick some demand pairs:
        int pointer = 0;
        HashSet<String> used = new HashSet<>();
        for (int i = 0; i < 15; i++) {
            while (srcs[pointer].equals(dsts[pointer]) || used.contains(srcs[pointer] + "-" + dsts[pointer])) {
                pointer++;
            }

            System.out.println(String.format("%s,%s,%d,%d,Firewall",
                    srcs[pointer],
                    dsts[pointer],
                    (long) (10000.0 + r.nextDouble() * 90000.0),
                    500));
            used.add(srcs[pointer] + "-" + dsts[pointer]);

            pointer++;
        }
    }
}
