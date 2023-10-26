package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.impl.solver.DefaultSolutionManager;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolverManager;
import ai.timefold.solver.quarkus.testdata.nodesharing.constraints.TestdataQuarkusNodeSharingConstraintProvider;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Run maven build with
 *
 * <pre>
 * mvn clean test -Dquarkus.debug.transformed-classes-dir=target/dump-transformed-classes
 * </pre>
 *
 * to see the transformed classes.
 */
class TimefoldProcessorNodeSharingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class, TestdataQuarkusNodeSharingConstraintProvider.class));

    @Inject
    SolverFactory<TestdataQuarkusSolution> solverFactory;
    @Inject
    SolverManager<TestdataQuarkusSolution, Long> solverManager;
    @Inject
    ScoreManager<TestdataQuarkusSolution, SimpleScore> scoreManager;
    @Inject
    SolutionManager<TestdataQuarkusSolution, SimpleScore> solutionManager;

    @Test
    void singletonSolverFactory() {
        assertNotNull(solverFactory);
        assertSame(((DefaultSolverFactory<TestdataQuarkusSolution>) solverFactory).getScoreDirectorFactory(),
                ((DefaultSolutionManager<TestdataQuarkusSolution, SimpleScore>) solutionManager).getScoreDirectorFactory());
        assertNotNull(solverManager);
        // There is only one SolverFactory instance
        assertSame(solverFactory, ((DefaultSolverManager<TestdataQuarkusSolution, Long>) solverManager).getSolverFactory());
        assertNotNull(solutionManager);
    }

}
