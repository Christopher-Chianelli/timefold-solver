package ai.timefold.solver.examples.cloudbalancing.app;

import java.io.File;
import java.time.Duration;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudBalance;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudProcess;
import ai.timefold.solver.examples.cloudbalancing.persistence.CloudBalanceSolutionFileIO;
import ai.timefold.solver.examples.cloudbalancing.swingui.CloudBalancingPanel;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

/**
 * For an easy example, look at {@link CloudBalancingHelloWorld} instead.
 */
public class CloudBalancingApp extends CommonApp<CloudBalance> {

    public static final String SOLVER_CONFIG = "ai/timefold/solver/examples/cloudbalancing/cloudBalancingSolverConfig.xml";

    public static final String DATA_DIR_NAME = "cloudbalancing";

    public static void main(String[] args) {
        SolverFactory<CloudBalance> solverFactory = SolverFactory.createFromXmlResource(SOLVER_CONFIG);
        Solver<CloudBalance> solver = solverFactory.buildSolver();
        CloudBalance solution = solver.solve(new CloudBalanceSolutionFileIO()
                .read(new File("data/cloudbalancing/unsolved/1600computers-4800processes.json")));
        CloudProcess process = solution.getProcessList().get(0);
        process.setComputer(null);

        for (int i = 0; i < 16; i = Math.max(1, i * 2)) {
            run(solution, process, i);
        }
    }

    private static void run(CloudBalance solution, CloudProcess process, int i) {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(SOLVER_CONFIG);
        solverConfig.setMoveThreadCount(i == 0 ? "NONE" : Integer.toString(i));
        SolverFactory<CloudBalance> factory = SolverFactory.create(solverConfig);
        SolutionManager<CloudBalance, HardSoftScore> solutionManager = SolutionManager.create(factory);
        long totalTime = 0;
        for (int j = 0; j < 1_000; j++) {
            long startTime = System.nanoTime();
            var recommendedFitList =
                    solutionManager.recommendFit(solution, process, CloudProcess::getComputer);
            long endTime = System.nanoTime() - startTime;
            totalTime += endTime;
        }
        System.out.printf("Move thread count: %2d - Average time: %d millis%n",
                i, Duration.ofNanos(totalTime / 1_000).toMillis());
    }

    public CloudBalancingApp() {
        super("Cloud balancing",
                "Assign processes to computers.\n\n" +
                        "Each computer must have enough hardware to run all of its processes.\n" +
                        "Each used computer inflicts a maintenance cost.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                CloudBalancingPanel.LOGO_PATH);
    }

    @Override
    protected CloudBalancingPanel createSolutionPanel() {
        return new CloudBalancingPanel();
    }

    @Override
    public SolutionFileIO<CloudBalance> createSolutionFileIO() {
        return new CloudBalanceSolutionFileIO();
    }

}
