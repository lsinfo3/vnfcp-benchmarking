import de.uniwue.vnfcpBench.generators.GridGraphProblem;
import de.uniwue.vnfcpBench.model.solution.ParetoFrontier;
import de.uniwue.vnfcpBench.model.solution.GridGraphSolution;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class GridGraphTest {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);

        test();
        //eval();
        //eval2();
    }

    public static void eval2() throws Exception {
        long seed = new Random().nextLong();
        System.out.println("Random seed: " + seed);

        long prefix = System.currentTimeMillis();
        String baseDir = "/home/alex/w/17/benchmark-vnfcp-generator/eval/" + prefix;
        Files.createDirectories(Paths.get(baseDir));
        Files.write(Paths.get(baseDir + "/seed"), (""+seed).getBytes());

        int numProblems = 20;

        // Scale rho or cpu while keeping the rest:
        for (int problemNr = 0; problemNr < numProblems; problemNr++) {
            double rho = 0.2 + 0.6 * problemNr / (numProblems - 1);
            double nk = Math.sqrt(10.0 + problemNr);
            int n = problemNr + 4;
            int k = (int) Math.round((double) 48 / n);
            GridGraphProblem msgp = new GridGraphProblem(40, 40, 4, 4, 3, 3, rho, 2, new Random(seed));
            //GridGraphProblem msgp = new GridGraphProblem(14, 14, k, k, n, n, 0.8, new Random(seed));
            //GridGraphProblem msgp = new GridGraphProblem(14, 14, 4, 4, 3, 3, 0.8, problemNr+1, new Random(seed));

            String dir = baseDir + "/" + problemNr;
            Files.createDirectories(Paths.get(dir));

            OutputStream topo = new FileOutputStream(dir + "/topology");
            OutputStream vnfs = new FileOutputStream(dir + "/vnfLib");
            OutputStream reqs = new FileOutputStream(dir + "/requests");
            GridGraphProblem.GridGraphInstance pi = msgp.generate(topo, vnfs, reqs);
            ParetoFrontier<GridGraphSolution> pf = msgp.getSolutions(pi);

            PrintStream optimal = new PrintStream(new FileOutputStream(dir + "/optimal"));
            optimal.println(String.format("# %d requests on %d nodes. [seed=%d, k=%d, m=%d, n=%d, rho=%.2f]",
                    pi.reqs.length,
                    pi.ng.getNodes().size(),
                    seed,
                    pi.k,
                    pi.m,
                    pi.n,
                    rho));
            optimal.println("cpu,hopcount");
            for (GridGraphSolution solution : pf) {
                optimal.println(Arrays.stream(solution.getObj()).mapToObj(d -> ""+d).collect(Collectors.joining(",")));
            }
            optimal.close();

            System.out.println("Generated problem " + (problemNr+1) + "/" + numProblems);
        }
    }

    public static void eval() throws Exception {
        long seed = new Random().nextLong();
        System.out.println("Random seed: " + seed);

        long prefix = System.currentTimeMillis();
        String baseDir = "/home/alex/w/itc-vnfcp-benchmark-generator/eval/" + prefix;
        Files.createDirectories(Paths.get(baseDir));
        Files.write(Paths.get(baseDir + "/seed"), (""+seed).getBytes());

        int numProblems = 50;
        GridGraphProblem msgp = new GridGraphProblem(10, 25, 3, 8, 4, 10, 0.8, new Random(seed));

        for (int problemNr = 0; problemNr < numProblems; problemNr++) {
            String dir = baseDir + "/" + problemNr;
            Files.createDirectories(Paths.get(dir));

            OutputStream topo = new FileOutputStream(dir + "/topology");
            OutputStream vnfs = new FileOutputStream(dir + "/vnfLib");
            OutputStream reqs = new FileOutputStream(dir + "/requests");
            GridGraphProblem.GridGraphInstance pi = msgp.generate(topo, vnfs, reqs);
            ParetoFrontier<GridGraphSolution> pf = msgp.getSolutions(pi);

            PrintStream optimal = new PrintStream(new FileOutputStream(dir + "/optimal"));
            optimal.println(String.format("# %d requests on %d nodes. [seed=%d, k=%d, m=%d, n=%d, rho=%.2f]",
                    pi.reqs.length,
                    pi.ng.getNodes().size(),
                    seed,
                    pi.k,
                    pi.m,
                    pi.n,
                    0.8));
            optimal.println("cpu,hopcount");
            for (GridGraphSolution solution : pf) {
                optimal.println(Arrays.stream(solution.getObj()).mapToObj(d -> ""+d).collect(Collectors.joining(",")));
            }
            optimal.close();

            System.out.println("Generated problem " + (problemNr+1) + "/" + numProblems);
        }
    }

    public static void test() throws Exception {
        long seed = new Random().nextLong();
        GridGraphProblem msgp = new GridGraphProblem(7, 7, 3, 3, 6, 6, 0.8, new Random(seed));

        String base = "res/msgp3/";
        OutputStream topo = new FileOutputStream(base + "topology");
        OutputStream vnfs = new FileOutputStream(base + "vnfLib");
        OutputStream reqs = new FileOutputStream(base + "requests");
        PrintStream psDotTopo = new PrintStream(new FileOutputStream(base + "topology.dot"));
        PrintStream optimal = new PrintStream(new FileOutputStream(base + "optimal"));
        GridGraphProblem.GridGraphInstance pi = msgp.generate(topo, vnfs, reqs);
        ParetoFrontier<GridGraphSolution> pf = msgp.getSolutions(pi);

        psDotTopo.println(pi.ng.toDotFile());
        psDotTopo.close();

        System.out.println(String.format("Generated %d requests on %d nodes. [seed=%d, k=%d, m=%d, n=%d, rho=%.2f]",
                pi.reqs.length,
                pi.ng.getNodes().size(),
                seed,
                pi.k,
                pi.m,
                pi.n,
                0.8));

        optimal.println(String.format("# %d requests on %d nodes. [seed=%d, k=%d, m=%d, n=%d, rho=%.2f]",
                pi.reqs.length,
                pi.ng.getNodes().size(),
                seed,
                pi.k,
                pi.m,
                pi.n,
                0.8));
        optimal.println("cpu,hopcount");
        System.out.println("Pareto frontier [" + pf.size() + " elements]:");
        for (GridGraphSolution solution : pf) {
            optimal.println(Arrays.stream(solution.getObj()).mapToObj(d->""+d).collect(Collectors.joining(",")));
            System.out.println(Arrays.toString(solution.getObj()));
        }
        optimal.close();
    }
}
