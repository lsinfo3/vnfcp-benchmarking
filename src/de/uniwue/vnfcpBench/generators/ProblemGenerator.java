package de.uniwue.vnfcpBench.generators;

import de.uniwue.vnfcpBench.model.solution.ParetoFrontier;
import de.uniwue.vnfcpBench.model.ProblemInstance;

import java.io.OutputStream;

public interface ProblemGenerator {
	ProblemInstance generate(OutputStream topology, OutputStream vnfLib, OutputStream requests);
	ParetoFrontier getSolutions(ProblemInstance inst);
}
