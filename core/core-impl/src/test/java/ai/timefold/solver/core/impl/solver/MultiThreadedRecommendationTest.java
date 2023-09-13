package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.testdata.domain.shadow.TestdataShadowedEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.shadow.TestdataShadowedEntity;
import ai.timefold.solver.core.impl.testdata.domain.shadow.TestdataShadowedSolution;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class MultiThreadedRecommendationTest {

    @ArgumentsSource(MultiThreadedArgumentProvider.class)
    @ParameterizedTest
    @ResourceLock("cpu")
    public void testMultithreadedRecommendation(int threadCount) {
        int valueSize = 5_000_000;
        var solution = TestdataShadowedSolution.generateSolution(valueSize, 3);
        var uninitializedEntity = solution.getEntityList().get(2);
        var unassignedValue = uninitializedEntity.getValue();
        uninitializedEntity.setValue(null);

        var solverConfig = new SolverConfig();
        solverConfig.withMoveThreadCount(Integer.toString(threadCount));
        solverConfig.withSolutionClass(TestdataShadowedSolution.class);
        solverConfig.withEntityClasses(TestdataShadowedEntity.class);
        solverConfig.withEasyScoreCalculatorClass(TestdataShadowedEasyScoreCalculator.class);
        var solutionManager = SolutionManager.create(SolverFactory.create(solverConfig));
        assertThat(solutionManager).isNotNull();
        var recommendationList = solutionManager.recommendFit(solution, uninitializedEntity, TestdataShadowedEntity::getValue);

        // Three values means there need to be three recommendations.
        assertThat(recommendationList).hasSize(valueSize);
    }

    public static class MultiThreadedArgumentProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return IntStream.of(1, 2, 4, 8, 16).mapToObj(Arguments::of);
        }
    }
}
