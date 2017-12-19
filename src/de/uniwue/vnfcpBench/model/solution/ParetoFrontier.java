package de.uniwue.vnfcpBench.model.solution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * This class computes the Pareto Frontier from a collection of solutions.
 *
 * @author alex
 */
public class ParetoFrontier<T extends Solution> extends ArrayList<T> {
    /**
     * Calls the superior constructor (ArrayList).
     *
     * @param initialCapacity (see ArrayList)
     */
    public ParetoFrontier(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Calls the superior constructor (ArrayList).
     */
    public ParetoFrontier() {
    }

    /**
     * Calls the superior constructor (ArrayList).
     *
     * @param c (see ArrayList)
     */
    public ParetoFrontier(Collection<? extends T> c) {
        super(c);
    }

    /**
     * Inserts a new solution into the existing Pareto Frontier, if it is not
     * dominated by another solution already.
     * (If it is dominated, the Pareto Frontier remains unchanged.)
     * Further, deletes all points in the current Pareto Frontier that are dominated by the new point.
     * (May also change the order of elements in the list.)
     *
     * @param newSolution New solution that shall be inserted..
     * @return <tt>null</tt>, if the new solution was not inserted;
     * otherwise, an ArrayList with all removed points is returned.
     */
    public ArrayList<T> updateParetoFrontier(T newSolution) {
        ArrayList<T> removed = new ArrayList<>();
        int i = 0;
        while (i < size()) {
            double[] iVector = get(i).getObj();
            double[] nVector = newSolution.getObj();
            int dominance = getDominance(iVector, nVector);

            // Is the new point dominated by the i-th solution? -> Abort.
            if (dominance == -1 || Arrays.equals(nVector, iVector)) return removed;

            // Is the i-th solution dominated by the new point? -> Remove i-th element.
            // (Changes list's order for performance reasons.)
            else if (dominance == +1) {
                // Switch i-th element with the last:
                T temp = get(i);
                set(i, get(size()-1));
                set(size()-1, temp);

                // Remove last:
                removed.add(remove(size()-1));
            }
            else {
                i++;
            }
        }

        // Solution is not dominated? -> Insert.
        // All newly dominated points are already removed here.
        add(newSolution);

        return removed;
    }

    /**
     * Checks whether one vector dominates the other.
     *
     * @param a First objective vector.
     * @param b Second objective vector.
     * @return -1, if <tt>a</tt> is better than <tt>b</tt>...
     * +1, if <tt>b</tt> is better than <tt>a</tt>...
     * 0, if they are incomparable or indifferent.
     */
    public static int getDominance(double[] a, double[] b) {
        // Is a dominated by b?
        boolean dominated = true;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < b[i]) {
                dominated = false;
                break;
            }
        }
        if (dominated) return +1;

        // Is b dominated by a?
        dominated = true;
        for (int i = 0; i < a.length; i++) {
            if (b[i] < a[i]) {
                dominated = false;
                break;
            }
        }
        if (dominated) return -1;

        return 0;
    }
}
