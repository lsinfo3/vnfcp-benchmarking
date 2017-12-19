package de.uniwue.vnfcpBench.generators;

import de.uniwue.vnfcpBench.model.*;
import de.uniwue.vnfcpBench.model.factory.TopologyFileReader;
import de.uniwue.vnfcpBench.model.solution.*;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DynamicResourceDistribution implements ProblemGenerator {
	private NetworkGraph baseGraph;
	private VnfLib vnfLib;
	private int[] possibleLocations;
	private int[] usedLocations;
	private int[] instances;
	private double[] requestedBandwidths;
	private Random rand;

	public DynamicResourceDistribution(NetworkGraph baseGraph, VnfLib vnfLib, int[] possibleLocations, int[] usedLocations, int[] instances, double[] requestedBandwidths, Random rand) {
		this.baseGraph = Objects.requireNonNull(baseGraph);
		this.vnfLib = Objects.requireNonNull(vnfLib);
		this.possibleLocations = Objects.requireNonNull(possibleLocations);
		this.usedLocations = Objects.requireNonNull(usedLocations);
		this.instances = Objects.requireNonNull(instances);
		this.requestedBandwidths = Objects.requireNonNull(requestedBandwidths);
		this.rand = Objects.requireNonNull(rand);

		verifyRanges(instances);
		verifyRanges(possibleLocations);
		verifyRanges(usedLocations);
		verifyRanges(requestedBandwidths);

		if (vnfLib.getAllVnfs().size() != 1) {
			throw new IllegalArgumentException("only one VNF type is currently supported (" + vnfLib.getAllVnfs().size() + " given)");
		}
		for (VNF v : vnfLib.getAllVnfs()) {
			if (v.processingCapacity < requestedBandwidths[0]) {
				throw new IllegalArgumentException("minimum requested bandwidth (" + requestedBandwidths[0] + ") bigger than VNF capacity (" + v.processingCapacity + ")");
			}
		}
	}

	private static void verifyRanges(int[] array) {
		verifyRanges(Arrays.stream(array).mapToDouble(i -> i).toArray());
	}

	private static void verifyRanges(double[] array) {
		if (array.length != 2) {
			throw new IllegalArgumentException("illegal range tuple: array.length = " + array.length);
		}
		if (array[0] < 1) {
			throw new IllegalArgumentException("illegal range tuple: array[0] = " + array[0]);
		}
		if (array[0] > array[1]) {
			throw new IllegalArgumentException("illegal range tuple: array[0] > array[1]: " + array[0] + " > " + array[1]);
		}
	}

	public DynamicResourceDistribution(NetworkGraph baseGraph, int numVnfs, double[] capacities, int[] possibleLocations, int[] usedLocations, int[] instances, double[] requestedBandwidths, Random rand) {
		this(baseGraph, generateVnfLib(numVnfs, capacities, rand), possibleLocations, usedLocations, instances, requestedBandwidths, rand);
	}

	public DynamicResourceDistribution(NetworkGraph baseGraph, int[] possibleLocations, int[] usedLocations, int[] instances, double[] requestedBandwidths, Random rand) {
		this(baseGraph, 1, new double[]{500.0, 700.0}, possibleLocations, usedLocations, instances, requestedBandwidths, rand);
	}

	@Override
	public ProblemInstance generate(OutputStream topoStream, OutputStream vnfStream, OutputStream reqStream) {
		// Find locations for instances
		int numUsedLocations = usedLocations[0] + rand.nextInt(usedLocations[1] - usedLocations[0] + 1);
		int numPossibleLocations = possibleLocations[0] + rand.nextInt(possibleLocations[1] - possibleLocations[0] + 1);
		if (numPossibleLocations < numUsedLocations) numPossibleLocations = numUsedLocations;
		Node[] allNodes = baseGraph.getNodes().values().toArray(new Node[baseGraph.getNodes().size()]);
		Collections.shuffle(Arrays.asList(allNodes), rand);
		Node[] cpuLocas = Arrays.copyOf(allNodes, numPossibleLocations);
		Node[] usedLocas = Arrays.copyOf(cpuLocas, numUsedLocations);

		// Generate some instances
		HashMap<Node, LinkedList<VnfInstance>> deployment = new HashMap<>();
		for (VNF vnf : vnfLib.getAllVnfs()) {
			int numInstances = instances[0] + rand.nextInt(instances[1] - instances[0] + 1);
			for (int i = 0; i < numInstances; i++) {
				Node location = usedLocas[rand.nextInt(usedLocas.length)];
				LinkedList<VnfInstance> currentList = deployment.computeIfAbsent(location, k -> new LinkedList<>());
				currentList.add(new VnfInstance(location, vnf));
			}
		}

		// Generate requests through these instances
		ArrayList<TrafficAssignment> assigs = new ArrayList<>();
		HashMap<Node, LinkedList<VnfInstance>> deploymentCopy = new HashMap<>();
		for (Map.Entry<Node, LinkedList<VnfInstance>> e : deployment.entrySet()) {
			deploymentCopy.put(e.getKey(), new LinkedList<>(e.getValue()));
		}

		int id = -1;
		while (deploymentCopy.size() != 0) {
			id++;
			int src = rand.nextInt(allNodes.length);
			int dst = rand.nextInt(allNodes.length-1);
			if (dst >= src) dst++;
			Node srcNode = allNodes[src];
			Node dstNode = allNodes[dst];

			// Find shortest path through 1 instance
			// TODO: This needs to be changed when introducing multiple VNFs.
			TrafficFlow flow = findShortestPathWithInstance(baseGraph, deploymentCopy, srcNode, dstNode);

			// Select requested bandwidth
			double maxDemandBandw = requestedBandwidths[1];
			if (flow.instances.length > 0) {
				maxDemandBandw = Math.min(maxDemandBandw, Arrays.stream(flow.instances).mapToDouble(VnfInstance::getRemainingCapacity).min().getAsDouble());
			}
			double bandw = maxDemandBandw;
			if (maxDemandBandw > requestedBandwidths[0]) {
				bandw = requestedBandwidths[0] + rand.nextDouble() * (maxDemandBandw - requestedBandwidths[0]);
			}

			// Ensure optimality
			// TODO: This needs to be changed when introducing multiple VNFs.
			for (VnfInstance inst : flow.instances) {
				// Would there be no room for more requests in that instance? --> Ensure that it fills up.
				if (inst.getRemainingCapacity() - bandw < requestedBandwidths[0]) {
					// Is the remaining capacity still within request-max-bounds?
					if (inst.getRemainingCapacity() <= maxDemandBandw) {
						bandw = inst.getRemainingCapacity();
					}
					// Would two smaller requests still fit? --> Just make this one small enough.
					else if (inst.getRemainingCapacity() >= 2.0 * requestedBandwidths[0]) {
						bandw = requestedBandwidths[0] + rand.nextDouble() * (inst.getRemainingCapacity() - 2.0 * requestedBandwidths[0]);
					}
					// Meh... just use more bandwidth than allowed this once.
					else {
						bandw = inst.getRemainingCapacity();
					}
				}
			}

			// Select delay requirement
			double maxDelay = flow.getDelay() * 1.5;

			// Create TrafficRequest
			VNF[] vnfSeq = Arrays.stream(flow.instances).map(i -> i.vnf).toArray(VNF[]::new);
			TrafficRequest req = new TrafficRequest(id, srcNode, dstNode, bandw * 0.99, maxDelay, vnfSeq);
			assigs.add(new TrafficAssignment(req, flow));

			// Refresh data structures
			for (VnfInstance inst : flow.instances) {
				inst.usedCapacity += bandw;
				LinkedList<VnfInstance> list = deploymentCopy.get(inst.node);
				if (inst.getRemainingCapacity() < requestedBandwidths[0]) {
					list.remove(inst);
				}
				if (list.isEmpty()) {
					deploymentCopy.remove(inst.node);
				}
			}
		}

		Collections.shuffle(assigs, rand);
		TrafficRequest[] reqs = assigs.stream().map(a -> a.req).toArray(TrafficRequest[]::new);

		// Generate graph including resources
		NetworkGraph ng = new NetworkGraph();
		double resources = baseGraph.getNodes().values().stream()
				.mapToDouble(n ->
						deployment.getOrDefault(n, new LinkedList<>()).stream().mapToDouble(v -> v.vnf.cpuRequired).sum()
				)
				.max().orElse(0.0) * 2.0;
		HashSet<Node> cpuLocasSet = new HashSet<>(Arrays.asList(cpuLocas));
		for (Node n : baseGraph.getNodes().values()) {
			if (cpuLocasSet.contains(n)) {
				ng.addNode(n.name, resources, resources, resources);
			}
			else {
				ng.addNode(n.name, 0.0, 0.0, 0.0);
			}
		}

		// Generate links with sufficient capacity
		HashMap<Link, Double> reqCapacity = new HashMap<>();
		for (TrafficAssignment assig : assigs) {
			for (Hop h : assig.flow.path) {
				if (h.previous != null) {
					reqCapacity.put(h.previous, reqCapacity.getOrDefault(h.previous, 0.0) + assig.req.bandwidthDemand);
				}
			}
		}
		for (Link l : baseGraph.getLinks()) {
			//ng.addLink(ng.getNodes().get(l.node1.name), ng.getNodes().get(l.node2.name), l.bandwidth, l.delay);
			ng.addLink(ng.getNodes().get(l.node1.name), ng.getNodes().get(l.node2.name), Math.ceil(reqCapacity.getOrDefault(l, l.bandwidth) * 5.0), l.delay);
		}

		// Export stuff
		if (topoStream != null) {
			PrintStream t = new PrintStream(topoStream);
			t.println(ng.toString());
		}
		if (vnfStream != null) {
			PrintStream v = new PrintStream(vnfStream);
			v.println(vnfLib.toString());
		}
		if (reqStream != null) {
			PrintStream r = new PrintStream(reqStream);
			r.println("# Ingress-ID, Egress-ID, Min-Bandwidth, Max-Delay, VNF, VNF, VNF, ...");
			for (TrafficRequest req : reqs) {
				r.println(req.toCsv());
			}
		}

		ProblemInstance pi = new ProblemInstance(ng, vnfLib, reqs);
		ParetoFrontier<DynamicDistSolution> frontier = new ParetoFrontier<>();
		frontier.add(new DynamicDistSolution(assigs.toArray(new TrafficAssignment[assigs.size()])));
		pi.solution = frontier;

		return pi;
	}

	private TrafficFlow findShortestPathWithInstance(NetworkGraph baseGraph, HashMap<Node, LinkedList<VnfInstance>> deployment, Node srcNode, Node dstNode) {
		LinkedList<Hop> ret = new LinkedList<>();
		HashMap<Node, HashMap<Node, Node.Att>> bp = baseGraph.getDijkstraBackpointers();

		// Find best middle choice
		Node mid = null;
		double dst = -1;
		for (Node n : deployment.keySet()) {
			double currentDst = bp.get(srcNode).get(n).delay + bp.get(n).get(dstNode).delay;
			if (mid == null || dst > currentDst) {
				mid = n;
				dst = currentDst;
			}
		}

		// Create path
		HashMap<Node, Node.Att> pointers = bp.get(mid);
		Node.Att c = pointers.get(dstNode);
		VnfInstance inst = null;
		while (c.pi != null) {
			ret.addFirst(new Hop(c.node, c.pi, inst));
			inst = null;
			c = pointers.get(c.pi.getOther(c.node));
		}

		LinkedList<VnfInstance> midsInstances = deployment.get(mid);
		inst = midsInstances.get(rand.nextInt(midsInstances.size()));

		pointers = bp.get(srcNode);
		c = pointers.get(mid);
		while (c.pi != null) {
			ret.addFirst(new Hop(c.node, c.pi, inst));
			inst = null;
			c = pointers.get(c.pi.getOther(c.node));
		}

		ret.addFirst(new Hop(srcNode, null, inst));

		return new TrafficFlow(srcNode, dstNode, ret.toArray(new Hop[ret.size()]));
	}

	@Override
	public ParetoFrontier<DynamicDistSolution> getSolutions(ProblemInstance inst) {
		return inst.solution;
	}

	private static VnfLib generateVnfLib(int numVnfs, double[] capacities, Random rand) {
		verifyRanges(capacities);

		VnfLib lib = new VnfLib();
		for (int i = 0; i < numVnfs; i++) {
			double resRequired = rand.nextInt(8) + 1.0;
			double step = 50.0;
			int numSteps = (int) Math.floor((capacities[1] - capacities[0]) / step) + 1;
			double processingCap = capacities[0] + rand.nextInt(numSteps) * step;

			lib.addVnf("Vnf"+i, new VNF[]{new VNF(
					"Vnf"+i,
					resRequired,
					resRequired,
					resRequired,
					50.0,
					processingCap,
					-1
			)});
		}
		return lib;
	}

	public static void testOnce(String base, String baseOut, int[] possibleLocations, int[] usedLocations, int[] instances, double[] requestedBandwidths, long seed) throws Exception {
		// Read topology and request files.
		Files.createDirectories(Paths.get(baseOut));
		NetworkGraph ng = TopologyFileReader.readFromFile(base + "topology");
		//VnfLib lib = VnfLibReader.readFromFile(base + "vnfLib");

		Random rand = new Random(seed);
		DynamicResourceDistribution generator = new DynamicResourceDistribution(ng, possibleLocations, usedLocations, instances, requestedBandwidths, rand);

		OutputStream outTopo = new FileOutputStream(baseOut + "outTopo");
		OutputStream outVnfs = new FileOutputStream(baseOut + "outVnfs");
		OutputStream outReqs = new FileOutputStream(baseOut + "outReqs");
		ProblemInstance pi = generator.generate(outTopo, outVnfs, outReqs);
		String config = String.format("seed=%s\npossibleLocations=%s\nusedLocations=%s\ninstances=%s\nrequestedBandwidths=%s\n",
				""+seed,
				Arrays.toString(possibleLocations),
				Arrays.toString(usedLocations),
				Arrays.toString(instances),
				Arrays.toString(requestedBandwidths)
		);
		Files.write(Paths.get(baseOut + "config"), config.getBytes());

		OutputStream outSol = new FileOutputStream(baseOut + "solution");
		OutputStream outSolCsv = new FileOutputStream(baseOut + "solution_csv");
		PrintStream printSol = new PrintStream(outSol);
		PrintStream printSolCsv = new PrintStream(outSolCsv);
		DynamicDistSolution sol = generator.getSolutions(pi).get(0);

		int instancesCsv = 0;
		double cpuCsv = 0.0;
		int hopsCsv = 0;
		double delayCsv = 0.0;

		printSol.println("# Instances");
		HashMap<Node, HashMap<VNF, HashSet<VnfInstance>>> instMap = new HashMap<>();
		for (Node n : ng.getNodes().values()) {
			instMap.put(n, new HashMap<>());
		}
		for (TrafficAssignment assig : sol.assigs) {
			for (VnfInstance inst : assig.flow.instances) {
				HashSet<VnfInstance> currentSet = instMap.get(inst.node).computeIfAbsent(inst.vnf, k -> new HashSet<>());
				currentSet.add(inst);
			}
		}
		for (Map.Entry<Node, HashMap<VNF, HashSet<VnfInstance>>> e : instMap.entrySet()) {
			String depl = e.getValue().entrySet().stream().map(d -> d.getValue().size() + "x" + d.getKey().name).collect(Collectors.joining(", "));
			printSol.println("[" + e.getKey().name + "]: " + depl);

			instancesCsv += e.getValue().entrySet().stream().mapToInt(d -> d.getValue().size()).sum();
			cpuCsv += e.getValue().entrySet().stream().mapToDouble(d -> d.getValue().size() * d.getKey().cpuRequired).sum();
		}

		printSol.println();
		printSol.println("# Flows");
		for (TrafficAssignment assig : sol.assigs) {
			printSol.print("[" + assig.req.id + "]: " + assig.req.ingress.name + " -> " + assig.req.egress.name + " " + Arrays.toString(Arrays.stream(assig.req.vnfSequence).map(v -> v.name).toArray()) + ":");
			for (Hop h : assig.flow.path) {
				printSol.print("  " + h.currentNode.name + (h.inst != null ? "*" : ""));
			}
			printSol.println();

			hopsCsv += assig.flow.getHops();
			delayCsv += assig.flow.getDelay();
		}

		printSolCsv.println("instances;cpu;hops;delay");
		printSolCsv.println(instancesCsv + ";" + cpuCsv + ";" + hopsCsv + ";" + delayCsv);

		System.out.println(String.format("Created a problem with %d requests, %d CPU locations (%d used), and %d instances.\nSaved in %s.",
				pi.reqs.length,
				pi.ng.getNodes().values().stream().filter(n -> n.cpuCapacity > 0.0).count(),
				sol.getUsedNodes(),
				sol.getInstances(),
				baseOut));
	}

	public static void testMany(String base, String baseOut, long seed) throws Exception {
		double[] requestedBandwidths = new double[]{100, 200};
		Random rand = new Random(seed);

		for (int i = 1; i <= 50; i++) {
			int[] possibleLocations = new int[]{i, i};
			int[] usedLocations = new int[]{i, i};
			int[] instances = new int[]{250, 250};
			String baseOutNow = baseOut + "i" + i + "/";
			for (int j = 0; j < 10; j++) {
				testOnce(base, baseOutNow + "j" + j + "/", possibleLocations, usedLocations, instances, requestedBandwidths, rand.nextLong());
			}
		}

		for (int r = 1; r <= 100; r++) {
			int[] possibleLocations = new int[]{36, 36};
			int[] usedLocations = new int[]{36, 36};
			int[] instances = new int[]{r, r};
			String baseOutNow = baseOut + "r" + r + "/";
			for (int j = 0; j < 10; j++) {
				testOnce(base, baseOutNow + "j" + j + "/", possibleLocations, usedLocations, instances, requestedBandwidths, rand.nextLong());
			}
		}
	}

	public static void main(String[] args) throws Exception {
		long seed = new Random().nextLong();

//		String base = "/home/alex/w/17/benchmark-vnfcp-generator/java/VNFCP_benchmarking/res/problem_instances/internet2/";
//		String baseOut = base + "out/";
//		int[] possibleLocations = new int[]{6, 8};
//		int[] usedLocations = new int[]{3, 5};
//		int[] instances = new int[]{18, 26};
//		double[] requestedBandwidths = new double[]{100.0, 250.0};
//		testOnce(base, baseOut, possibleLocations, usedLocations, instances, requestedBandwidths, seed);

		String base = "/home/alex/w/17/benchmark-vnfcp-generator/java/VNFCP_benchmarking/res/problem_instances/germany/";
		String baseOut = "/mnt/Daten/evals/benchmark-vnfcp-generator/dynamic/" + System.currentTimeMillis() + "/";
		testMany(base, baseOut, seed);
	}
}
