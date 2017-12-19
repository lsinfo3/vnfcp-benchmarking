package de.uniwue.vnfcpBench.solvers.bruteForce;

import de.uniwue.vnfcpBench.model.Link;
import de.uniwue.vnfcpBench.model.Node;

import java.util.*;

/**
 * Contains static methods to faciliate TrafficFlow creation by
 * computing shortest paths and connecting given intermediate nodes.
 *
 * @author alex
 */
public class FlowUtils {
    /**
     * Performs a shortest path search (wrt. hops) from the given start (z.B. {@code req.ingress})
     * and returns a corresponding mapping (Node -> Backpointer).
     *
     * @param start Starting node of the search..
     * @return A path-mapping with minimal number of hops.
     */
    public static HashMap<Node, Node.Att> bfs(Node start) {
        Objects.requireNonNull(start);

        // Data structures
        HashMap<Node, Node.Att> att = new HashMap<>();
        LinkedList<Node.Att> q = new LinkedList<>();

        Node.Att startNode = new Node.Att(start, 1, 0, 0, null);
        q.add(startNode);
        att.put(start, startNode);

        // Actual bfs:
        while (!q.isEmpty()) {
            Node.Att u = q.poll();
            for (Link l : u.node.getNeighbours()) {
                Node vNode = l.getOther(u.node);

                // If the neighbor has not been visited yet:
                if (!att.containsKey(vNode)) {
                    Node.Att vNeu = new Node.Att(vNode, 1, u.h + 1, u.delay + l.delay, l);
                    q.add(vNeu);
                    att.put(vNode, vNeu);
                }
            }
        }

        return att;
    }

    /**
     * Performs a shortest path search (wrt. delay) from the given start (z.B. {@code req.ingress})
     * and returns a corresponding mapping (Node -> Backpointer).
     *
     * @param start Starting node of the search..
     * @return A path-mapping with minimal delay.
     */
    public static HashMap<Node, Node.Att> dijkstra(Node start) {
        Objects.requireNonNull(start);

        // Data structures
        HashMap<Node, Node.Att> att = new HashMap<>();
        PriorityQueue<Node.Att> q = new PriorityQueue<>(new Node.AttComparatorDelay());

        Node.Att startNode = new Node.Att(start, 1, 0, 0, null);
        q.add(startNode);
        att.put(start, startNode);

        // Actual Dijkstra:
        while (!q.isEmpty()) {
            Node.Att u = q.poll();
            if (u.color == 2) continue;

            for (Link l : u.node.getNeighbours()) {
                Node vNode = l.getOther(u.node);
                Node.Att v = (att.containsKey(vNode) ? att.get(vNode) : new Node.Att(vNode, 0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, null));

                if (v.delay > u.delay + l.delay) {
                    //v.color = 1;
                    //v.d = u.d + l.delay;
                    //v.pi = l;
                    Node.Att vNeu = new Node.Att(vNode, 1, u.h + 1, u.delay + l.delay, l);
                    q.add(vNeu);
                    att.put(vNode, vNeu);
                }
            }
            u.color = 2;
        }

        return att;
    }
}
