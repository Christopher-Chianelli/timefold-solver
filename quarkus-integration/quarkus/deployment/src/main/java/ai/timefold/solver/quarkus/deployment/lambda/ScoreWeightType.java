package ai.timefold.solver.quarkus.deployment.lambda;

import java.math.BigDecimal;

public enum ScoreWeightType {
    INTEGER(int.class),
    LONG(long.class),
    BIG_DECIMAL(BigDecimal.class);

    Class<?> weightClass;

    ScoreWeightType(Class<?> weightClass) {
        this.weightClass = weightClass;
    }

    public Class<?> getWeightClass() {
        return weightClass;
    }
}
