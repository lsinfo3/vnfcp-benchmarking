# VNFCP Benchmarking
Performance Benchmarking of Network Function Chain Placement Algorithms.

This repository contains the sources used for the work published in:
> A. Grigorjew, S. Lange, T. Zinner, and P. Tran-Gia,
> “Performance Benchmarking of Network Function Chain Placement Algorithms,”
> in the 19th International GI/ITG Conference on Measurement, Modelling and Evaluation of Computing Systems (MMB), Erlangen, Germany, 2018.

## Abstract

The Network Function Virtualization (NFV) paradigm enables new flexibility and possibilities in the deployment and operation of network services. Finding the best arrangement of such service chains poses new optimization problems, composed of a combination of placement and routing decisions. While there are many algorithms on this topic proposed in literature, this work is focused on their evaluation and on the choice of reference for meaningful assessments. Our contribution comprises two problem generation strategies with predefined optima for benchmarking purposes, supplemented by an integer program to obtain optimal solutions in arbitrary graphs, as well as a general overview of concepts and methodology for solving and evaluating problems. In addition, a short evaluation demonstrates their applicability and shows possible directions for future work in this area.

## Java Sources

This software includes all source files required to apply the problem generation strategies from the paper.
The algorithms that ran the actual optimization and were evaluated using these problems are not included.

## Dependencies

Only Java 8 is required to run most use cases.
For class `MinimizeCpu` (which equals the ILP from the paper), the Java API of Gurobi version `7.0.2` must be in the classpath.

## Configuration and Execution

For this proof-of-concept implementation, there is no dedicated configuration file or command line arguments available.
All configuration is done directly inside the source files.
It is recommended to open the project in a Java IDE of your choice, then edit & run the files directly.

The class `GridGraphTest` contains method calls to create a single, or multiple, problem instances of the Grid-Graph-Problem.
Class `de.uniwue.vnfcpBench.solvers.gurobi.MinimizeCpu` contains the implementation of the ILP solver with Gurobi. It is executed via its `main()` method at the end of the file.
Finally, class `de.uniwue.vnfcpBench.generators.DynamicResourceDistribution` contains two basic configurations in `testOne()` and `testMany()`, both called by its own `main()` method.

Note that, in all cases, at least the base folder of all files should be changed according to your setup.

## Authors

Alexej Grigorjew - <alexej.grigorjew@informatik.uni-wuerzburg.de>  
Stanislav Lange - <stanislav.lange@informatik.uni-wuerzburg.de>  
Thomas Zinner - <zinner@informatik.uni-wuerzburg.de>  
Phuoc Tran-Gia - <trangia@informatik.uni-wuerzburg.de>
