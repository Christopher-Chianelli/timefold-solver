package ai.timefold.solver.quarkus.deployment.lambda.ast;

public interface JoinerNode {
    JoinerDefinition.Combined<?, ?> getCombinedJoiners();
}
