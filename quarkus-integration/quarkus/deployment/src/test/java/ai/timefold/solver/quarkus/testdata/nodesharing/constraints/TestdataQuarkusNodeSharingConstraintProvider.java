package ai.timefold.solver.quarkus.testdata.nodesharing.constraints;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;

public class TestdataQuarkusNodeSharingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                constraint1(factory),
                constraint2(factory),
                constraint3(factory),
                constraint4(factory),
        };
    }

    Constraint constraint1(ConstraintFactory factory) {
        return factory.forEach(TestdataQuarkusEntity.class)
                .join(TestdataQuarkusEntity.class, Joiners.equal(TestdataQuarkusEntity::getValue))
                .filter((a, b) -> a != b)
                .penalize(SimpleScore.ONE)
                .asConstraint("Don't assign 2 entities the same value 1.");
    }

    Constraint constraint2(ConstraintFactory factory) {
        return factory.forEach(TestdataQuarkusEntity.class)
                .join(TestdataQuarkusEntity.class, Joiners.equal(TestdataQuarkusEntity::getValue))
                .filter((a, b) -> a != b)
                .penalize(SimpleScore.ONE)
                .asConstraint("Don't assign 2 entities the same value 2.");
    }

    Constraint constraint3(ConstraintFactory factory) {
        return factory.forEach(TestdataQuarkusEntity.class)
                .join(TestdataQuarkusEntity.class, Joiners.equal(TestdataQuarkusEntity::getValue))
                .filter((a, b) -> a == b)
                .penalize(SimpleScore.ONE)
                .asConstraint("Don't assign 2 entities the same value 3.");
    }

    Constraint constraint4(ConstraintFactory factory) {
        return factory.forEach(TestdataQuarkusEntity.class)
                .join(TestdataQuarkusEntity.class, Joiners.equal(TestdataQuarkusEntity::getValue))
                .filter((a, b) -> a == b)
                .join(TestdataQuarkusEntity.class, Joiners.equal((a, b) -> b.getValue(), TestdataQuarkusEntity::getValue))
                .filter((a, b, c) -> a == b)
                .penalize(SimpleScore.ONE)
                .asConstraint("Don't assign 2 entities the same value 4.");
    }

}
