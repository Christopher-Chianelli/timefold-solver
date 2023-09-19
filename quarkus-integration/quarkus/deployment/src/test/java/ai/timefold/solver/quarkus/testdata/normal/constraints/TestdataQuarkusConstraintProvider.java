package ai.timefold.solver.quarkus.testdata.normal.constraints;

import java.io.Serializable;
import java.util.function.BiPredicate;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;

public class TestdataQuarkusConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                a(factory),
                b(factory)
        };
    }

    public Constraint a(ConstraintFactory factory) {
        return factory.forEach(TestdataQuarkusEntity.class)
                .join(TestdataQuarkusEntity.class,
                        Joiners.equal((Serializable & Function<TestdataQuarkusEntity, Object>) TestdataQuarkusEntity::getValue))
                .filter((Serializable & BiPredicate<TestdataQuarkusEntity, TestdataQuarkusEntity>) (a, b) -> a != b)
                .penalize(SimpleScore.ONE)
                .asConstraint("Don't assign 2 entities the same value.");
    }

    public Constraint b(ConstraintFactory factory) {
        return factory.forEach(TestdataQuarkusEntity.class)
                .join(TestdataQuarkusEntity.class,
                        Joiners.equal((Serializable & Function<TestdataQuarkusEntity, Object>) TestdataQuarkusEntity::getValue))
                .filter((Serializable & BiPredicate<TestdataQuarkusEntity, TestdataQuarkusEntity>) (a, b) -> a != b)
                .penalize(SimpleScore.ONE)
                .asConstraint("Don't assign 2 entities the same value again.");
    }

}
