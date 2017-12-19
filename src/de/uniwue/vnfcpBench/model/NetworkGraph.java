package de.uniwue.vnfcpBench.model;

import de.uniwue.vnfcpBench.solvers.bruteForce.FlowUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Represents the topology of (physical) nodes and links where the placement is to be performed on.
 *
 * @author alex
 */
public class NetworkGraph {
    private HashMap<String, Node> nodes;
    private HashMap<Node, HashMap<Node, Node.Att>> backpointerBfs;
    private HashMap<Node, HashMap<Node, Node.Att>> backpointerDij;

    /**
     * Creates a new, empty graph.
     */
    public NetworkGraph() {
        nodes = new HashMap<>();
    }

    /**
     * Creates a new node with the given resources and adds it to the graph
     *
     * @param name        Name / ID of the new node.
     * @param cpuCapacity Available number of cores.
     * @param ramCapacity Available amount of RAM (Mb).
     * @param hddCapacity Available HDD resources (Gb).
     * @return Newly created Node object,
     */
    public Node addNode(String name, double cpuCapacity, double ramCapacity, double hddCapacity) {
        Node n = new Node(name, cpuCapacity, ramCapacity, hddCapacity);

        if (nodes.containsKey(name)) {
            throw new IllegalArgumentException("node " + n.name + " added twice");
        }

        nodes.put(name, n);
        backpointerDij = null;
        backpointerBfs = null;
        return n;
    }

    /**
     * Creates a new (undirected) {@link Link} between the given nodes and adds it to both nodes' neighbor lists.
     *
     * @param n1        First node of the link.
     * @param n2        Second node of the link.
     * @param bandwidth Available bandwidth. (Mbps)
     * @param delay     Latency of the link (μs).
     * @return Newly created Link object.
     */
    public Link addLink(Node n1, Node n2, double bandwidth, double delay) {
        backpointerDij = null;
        backpointerBfs = null;
        return n1.addNeighbour(n2, bandwidth, delay);
    }

    /**
     * Creates a new (directed) {@link Link} from n1 to n2 and adds it to n1's neighbor list.
     *
     * @param n1        First node of the link.
     * @param n2        Second node of the link.
     * @param bandwidth Available bandwidth. (Mbps)
     * @param delay     Latency of the link (μs).
     * @return Newly created Link object.
     */
    public Link addLinkDirected(Node n1, Node n2, double bandwidth, double delay) {
        backpointerDij = null;
        backpointerBfs = null;
        return n1.addNeighbourDirected(n2, bandwidth, delay);
    }

    /**
     * Returns the node map.
     *
     * @return A map with NodeName -> Node Object pointers.
     */
    public HashMap<String, Node> getNodes() {
        return nodes;
    }

    /**
     * Collects all links in the graph and returns the Collection.
     *
     * @return A HashSet containing all Links in the network.
     */
    public HashSet<Link> getLinks() {
        return nodes.values().stream()
                .map(Node::getNeighbours)
                .flatMap(HashSet::stream)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public String toString() {
        HashSet<Link> links = getLinks();
        StringBuilder sb = new StringBuilder("# Number of nodes, Number of links");
        sb.append("\n").append(nodes.size()).append(" ").append(links.size());
        sb.append("\n\n# Node-ID Cores RAM HDD");

        for (Node n : nodes.values()) {
            sb.append("\n").append(n.name).append(" ").append(n.cpuCapacity).append(" ").append(n.ramCapacity).append(" ").append(n.hddCapacity);
        }

        sb.append("\n\n# Node-ID Node-ID Bandwidth Delay");

        for (Link l : links) {
            sb.append("\n").append(l.node1.name).append(" ").append(l.node2.name)
                    .append(" ").append(l.bandwidth * 1000.0).append(" ").append(l.delay);
        }

        return sb.toString();
    }

    /**
     * Creates a DOT file from this NetworkGraph object.
     * This can be used to quickly draw the graph with graphviz methods.
     *
     * @return DOT file containing the graph.
     */
    public String toDotFile() {
        HashSet<Link> links = getLinks();
        StringBuilder sb = new StringBuilder("graph networkGraphTest {\n" +
                "  node [\n" +
                "    shape = \"circle\",\n" +
                "    style = \"filled\",\n" +
                "    fontsize = 12,\n" +
                "    fixedsize = true\n" +
                "  ];\n" +
                "\n" +
                "  edge [\n" +
                "    color = \"#bbbbbb\"\n" +
                "  ];\n" +
                "\n" +
                "  // nodes with CPU\n" +
                "  node [\n" +
                "    color = \"#007399\",\n" +
                "    fillcolor = \"#007399\",\n" +
                "    fontcolor = white\n" +
                "  ];\n");

        // Nodes with CPU resources:
        nodes.values().stream().filter(n -> n.cpuCapacity > 0.0).forEach(n -> sb.append("  ").append(n.name).append(";\n"));

        sb.append("\n" +
                "  // nodes without CPU\n" +
                "  node [\n" +
                "    color = \"#4dd2ff\",\n" +
                "    fillcolor = \"#4dd2ff\",\n" +
                "    fontcolor = black\n" +
                "  ];\n");

        // Nodes without CPU resources:
        nodes.values().stream().filter(n -> n.cpuCapacity == 0.0).forEach(n -> sb.append("  ").append(n.name).append(";\n"));

        sb.append("\n" +
                "  // edges\n");

        // Edges:
        links.forEach(l -> sb.append("  ")
                .append(l.node1.name)
                .append(" -- ")
                .append(l.node2.name)
                .append(" [ label = \"")
                .append(Math.round(l.delay))
                .append("\" ];\n"));

        sb.append("}");

        return sb.toString();
    }

    /**
     * Returns the content of a double without its decimal points as a String.
     *
     * @param d The original number.
     * @return The same number without decimal places.
     */
    public static String noDigits(double d) {
        return String.format("%.0f", d);
    }

    /**
     * Returns shortest path pointers after a BFS.
     *
     * @return Backpointers after performing BFS
     */
    public HashMap<Node, HashMap<Node, Node.Att>> getBfsBackpointers() {
        if (backpointerBfs == null) {
            backpointerBfs = new HashMap<>();
            for (Node n : nodes.values()) {
                backpointerBfs.put(n, FlowUtils.bfs(n));
            }
        }
        return backpointerBfs;
    }

    /**
     * Returns shortest path pointers after a Dijkstra search.
     *
     * @return Backpointers after performing Dijkstra
     */
    public HashMap<Node, HashMap<Node, Node.Att>> getDijkstraBackpointers() {
        if (backpointerDij == null) {
            backpointerDij = new HashMap<>();
            for (Node n : nodes.values()) {
                backpointerDij.put(n, FlowUtils.dijkstra(n));
            }
        }
        return backpointerDij;
    }
}
