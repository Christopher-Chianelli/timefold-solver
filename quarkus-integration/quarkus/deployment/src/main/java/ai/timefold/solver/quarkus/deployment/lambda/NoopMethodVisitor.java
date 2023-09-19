package ai.timefold.solver.quarkus.deployment.lambda;

import org.objectweb.asm.MethodVisitor;

public class NoopMethodVisitor extends MethodVisitor {
    protected NoopMethodVisitor(int api) {
        super(api);
    }
}
