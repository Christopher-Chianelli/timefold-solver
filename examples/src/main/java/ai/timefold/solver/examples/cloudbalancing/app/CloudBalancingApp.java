package ai.timefold.solver.examples.cloudbalancing.app;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudBalance;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudProcess;
import ai.timefold.solver.examples.cloudbalancing.persistence.CloudBalanceSolutionFileIO;
import ai.timefold.solver.examples.cloudbalancing.persistence.CloudBalancingGenerator;
import ai.timefold.solver.examples.cloudbalancing.swingui.CloudBalancingPanel;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

/**
 * For an easy example, look at {@link CloudBalancingHelloWorld} instead.
 */
public class CloudBalancingApp extends CommonApp<CloudBalance> {

    public static final String SOLVER_CONFIG = "ai/timefold/solver/examples/cloudbalancing/cloudBalancingSolverConfig.xml";

    public static final String DATA_DIR_NAME = "cloudbalancing";

    private static final int MAX_MOVE_THREAD_COUNT = 8;
    private static final int TRIALS_PER_DATASET = 100;

    public static void main(String[] args) {
        CloudBalancingGenerator generator = new CloudBalancingGenerator();
        CloudBalance solution100k = generator.createCloudBalance(100_000, 1);
        CloudBalance solution10k = generator.createCloudBalance(10_000, 1);
        CloudBalance solution1k = generator.createCloudBalance(1_000, 1);
        CloudBalance solution100 = generator.createCloudBalance(100, 1);
        var solutions = List.of(solution100, solution1k, solution10k, solution100k);

        for (var solution: solutions) {
            System.out.println("Fitting " + solution.getComputerList().size() + " computers");
            for (int moveThreadCount = 0;
                 moveThreadCount <= MAX_MOVE_THREAD_COUNT;
                 moveThreadCount = Math.max(1, moveThreadCount * 2)) {
                run(solution, solution.getProcessList().get(0), moveThreadCount);
            }
            System.out.println();
        }

    }

    private static void run(CloudBalance solution, CloudProcess process, int moveThreadCount) {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(SOLVER_CONFIG);
        solverConfig.setMoveThreadCount(moveThreadCount == 0 ? "NONE" : Integer.toString(moveThreadCount));
        SolverFactory<CloudBalance> factory = SolverFactory.create(solverConfig);
        SolutionManager<CloudBalance, HardSoftScore> solutionManager = SolutionManager.create(factory);
        long totalTime = 0;
        for (int trial = 0; trial < TRIALS_PER_DATASET; trial++) {
            long startTime = System.nanoTime();
            var recommendedFitList =
                    solutionManager.recommendFit(solution, process, CloudProcess::getComputer);
            long endTime = System.nanoTime() - startTime;
            totalTime += endTime;
        }
        System.out.printf("Move thread count: %2d - Average time: %2d nanos%n",
                moveThreadCount, totalTime / TRIALS_PER_DATASET);
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
