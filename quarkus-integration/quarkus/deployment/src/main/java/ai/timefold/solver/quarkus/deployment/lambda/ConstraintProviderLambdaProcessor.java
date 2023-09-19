package ai.timefold.solver.quarkus.deployment.lambda;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AbstractConstraintStreamNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AsConstraintNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.ConstraintStreamGraph;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.deployment.QuarkusClassVisitor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;

public class ConstraintProviderLambdaProcessor extends QuarkusClassVisitor {
    final Class<? extends ConstraintProvider> constraintProviderClass;
    final Map<Object, byte[]> lambdaToSerializedBytes = new IdentityHashMap<>();
    final ConstraintStreamGraph optimizedConstraintGraph;

    private static SerializedLambda getSerializedLambda(Serializable lambda) throws Exception {
        final Method method = lambda.getClass().getDeclaredMethod("writeReplace");
        method.setAccessible(true);
        return (SerializedLambda) method.invoke(lambda);
    }

    private static List<Object> readBytecodeOf(Object lambdaObject) {
        if (lambdaObject instanceof Serializable serializable) {
            try {
                SerializedLambda serializedLambda = getSerializedLambda(serializable);
                ClassReader classReader = new ClassReader(serializedLambda.getImplClass());
                LambdaClassVisitor lambdaClassVisitor =
                        new LambdaClassVisitor(Opcodes.ASM9, serializedLambda.getImplMethodName());
                classReader.accept(lambdaClassVisitor,
                        ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                return lambdaClassVisitor.getBytecode();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return List.of(new LambdaClassVisitor.UniqueObject(Float.NaN));
        }
    }

    public <Solution_> ConstraintProviderLambdaProcessor(SolverConfig solverConfig,
            int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
        this.constraintProviderClass = solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass();
        try {
            ConstraintProvider constraintProvider = constraintProviderClass.getConstructor().newInstance();
            @SuppressWarnings("unchecked")
            SolutionDescriptor<Solution_> solutionDescriptor = (SolutionDescriptor<Solution_>) SolutionDescriptor
                    .buildSolutionDescriptor(solverConfig.getSolutionClass(), solverConfig.getEntityClassList());

            AsmConstraintFactory<Solution_> asmConstraintFactory =
                    new AsmConstraintFactory<>(solutionDescriptor, solverConfig.getEnvironmentMode());

            @SuppressWarnings("unchecked")
            List<AsmConstraint<Solution_>> constraints = Arrays.stream(
                    constraintProvider.defineConstraints(asmConstraintFactory))
                    .map(constraint -> (AsmConstraint<Solution_>) constraint)
                    .toList();

            ConstraintStreamGraph constraintStreamGraph = ConstraintStreamGraph.fromConstraints(constraints);
            Map<Object, List<Object>> lambdaToBytecode = new IdentityHashMap<>();
            constraintStreamGraph.visitAll(node -> {
                node.visitBytecode(lambda -> lambdaToBytecode.put(lambda, readBytecodeOf(lambda)));
            });
            optimizedConstraintGraph = ConstraintStreamGraph.fromConstraints(constraints, lambdaToBytecode);
            optimizedConstraintGraph.visitAll(node -> {
                node.visitBytecode(lambda -> {
                    if (lambda instanceof Serializable && !lambdaToSerializedBytes.containsKey(lambda)) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                            objectOutputStream.writeObject(lambda);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        lambdaToSerializedBytes.put(lambda, byteArrayOutputStream.toByteArray());
                    }
                });
            });
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MethodVisitor visitMethod(int modifiers, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals("defineConstraints")
                && descriptor.equals(Type.getMethodDescriptor(Type.getType(Constraint[].class), Type.getType(
                        ConstraintFactory.class)))) {
            MethodVisitor methodVisitor = super.visitMethod(modifiers, name, descriptor, signature, exceptions);

            createDefineConstraintFunction(methodVisitor);

            return new NoopMethodVisitor(api);
        }
        return super.visitMethod(modifiers, name, descriptor, signature, exceptions);
    }

    private void createDefineConstraintFunction(MethodVisitor methodVisitor) {
        methodVisitor.visitCode();

        PriorityQueue<AbstractConstraintStreamNode> nodeQueue =
                new PriorityQueue<>(Comparator.comparing(optimizedConstraintGraph::getNestingLevel));
        IdentityHashMap<AbstractConstraintStreamNode, Integer> nodeToStreamVariableSlot = new IdentityHashMap<>();
        optimizedConstraintGraph.visitAll(nodeQueue::add);

        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(ArrayList.class));
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(ArrayList.class),
                "<init>", Type.getMethodDescriptor(Type.getType(void.class)), false);
        methodVisitor.visitVarInsn(Opcodes.ASTORE, 2);

        while (!nodeQueue.isEmpty()) {
            AbstractConstraintStreamNode node = nodeQueue.poll();
            node.writeBytecode(methodVisitor, optimizedConstraintGraph.getParents(node), nodeToStreamVariableSlot);
            if (!(node instanceof AsConstraintNode)) {
                // 0 is this; 1 is ConstraintFactory, 2 is constraint list
                int slot = nodeToStreamVariableSlot.size() + 3;
                nodeToStreamVariableSlot.put(node, slot);
                methodVisitor.visitVarInsn(Opcodes.ASTORE, slot);
            }
        }

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ArrayList.class),
                "toArray", Type.getMethodDescriptor(Type.getType(Object[].class)), false);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Constraint[].class));
        methodVisitor.visitInsn(Opcodes.ARETURN);

        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    public static void shareLambdas(SolverConfig solverConfig,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        transformers.produce(new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass().getName())
                .setVisitorFunction((className, visitor) -> new ConstraintProviderLambdaProcessor(solverConfig,
                        Opcodes.ASM9, visitor))
                .build());
    }

    public void loadLambda(MethodVisitor methodVisitor, Object lambda) {
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(ObjectInputStream.class));
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(ByteArrayInputStream.class));
        methodVisitor.visitLdcInsn(lambdaToSerializedBytes.get(lambda));
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(ByteArrayInputStream.class),
                "<init>", Type.getMethodDescriptor(Type.getType(void.class), Type.getType(byte[].class)),
                false);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(ObjectInputStream.class),
                "<init>", Type.getMethodDescriptor(Type.getType(void.class), Type.getType(InputStream.class)),
                false);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ObjectInputStream.class),
                "readObject", Type.getMethodDescriptor(Type.getType(Object.class)),
                false);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ObjectInputStream.class),
                "close", Type.getMethodDescriptor(Type.getType(void.class)),
                false);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(lambda.getClass()));
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }
}
