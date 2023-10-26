package ai.timefold.solver.quarkus.deployment;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;

public class NodeSharingConstraintProviderEnhancer {

    public void enhanceConstraintProvider(Class<? extends ConstraintProvider> constraintProviderClass,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        Map<String, String> methodNameToCanonicalMethod = new HashMap<>();

        // Use a custom comparator to compare suffix int value, so 9 comes before 10
        Map<String, LambdaSharingMethodVisitor.InvokeDynamicArgs> generatedFieldNameToInvokeDynamicArgs =
                new TreeMap<>(Comparator.comparingInt(name -> Integer.parseInt(name.substring(name.lastIndexOf('$') + 1))));
        // Need to use setInputTransformer to enforce that BytecodeRecordingClassVisitor
        // visitEnd is called before LambdaSharingClassVisitor begins visiting code
        transformers.produce(new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(constraintProviderClass.getName())
                .setInputTransformer(
                        (name, inputBytes) -> {
                            ClassWriter classWriter = new ClassWriter(Opcodes.ASM9);
                            BytecodeRecordingClassVisitor classVisitor =
                                    new BytecodeRecordingClassVisitor(classWriter, methodNameToCanonicalMethod);
                            ClassReader classReader = new ClassReader(inputBytes);
                            classReader.accept(classVisitor, Opcodes.ASM9);
                            return classWriter.toByteArray();
                        })
                .setPriority(0)
                .build());
        transformers.produce(new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(constraintProviderClass.getName())
                .setInputTransformer(
                        (name, inputBytes) -> {
                            ClassWriter classWriter = new ClassWriter(Opcodes.ASM9);
                            LambdaSharingClassVisitor classVisitor =
                                    new LambdaSharingClassVisitor(classWriter, name, methodNameToCanonicalMethod,
                                            generatedFieldNameToInvokeDynamicArgs);
                            ClassReader classReader = new ClassReader(inputBytes);
                            classReader.accept(classVisitor, Opcodes.ASM9);
                            return classWriter.toByteArray();
                        })
                .setPriority(1)
                .build());
        transformers.produce(new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(constraintProviderClass.getName())
                .setInputTransformer(
                        (name, inputBytes) -> {
                            ClassWriter classWriter = new ClassWriter(Opcodes.ASM9);
                            CreateSharedLambdaFieldsClassVisitor classVisitor =
                                    new CreateSharedLambdaFieldsClassVisitor(classWriter, name,
                                            generatedFieldNameToInvokeDynamicArgs);
                            ClassReader classReader = new ClassReader(inputBytes);
                            classReader.accept(classVisitor, Opcodes.ASM9);
                            return classWriter.toByteArray();
                        })
                .setPriority(2)
                .build());
    }

    private static class BytecodeRecordingClassVisitor extends ClassVisitor {
        private final Map<String, String> methodNameToBytecode = new HashMap<>();
        private final Map<String, String> methodIdToCanonicalMethodId;

        protected BytecodeRecordingClassVisitor(ClassVisitor classVisitor, Map<String, String> methodIdToCanonicalMethodId) {
            super(Opcodes.ASM9, classVisitor);
            this.methodIdToCanonicalMethodId = methodIdToCanonicalMethodId;
        }

        @Override
        public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String descriptor,
                final String signature,
                final String[] exceptions) {
            return new BytecodeRecordingMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions),
                    (bytecode) -> {
                        String key = LambdaSharingMethodVisitor.getMethodId(name, descriptor);
                        for (Map.Entry<String, String> existingBytecodeEntry : methodNameToBytecode.entrySet()) {
                            String existingKey = existingBytecodeEntry.getKey();
                            String existingMethodDescriptor = LambdaSharingMethodVisitor.getDescriptor(existingKey);
                            if (!descriptor.equals(existingMethodDescriptor)) {
                                continue;
                            }
                            String existingBytecode = existingBytecodeEntry.getValue();
                            if (existingBytecode.equals(bytecode)) {
                                methodIdToCanonicalMethodId.put(key, existingKey);
                                return;
                            }
                        }
                        methodNameToBytecode.put(key, bytecode);
                        methodIdToCanonicalMethodId.put(key, key);
                    });
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }
    }

    private static class LambdaSharingClassVisitor extends ClassVisitor {
        private final Map<String, String> methodIdToCanonicalMethodId;
        private final String classInternalName;
        private final Map<String, LambdaSharingMethodVisitor.InvokeDynamicArgs> generatedFieldNameToInvokeDynamicArgs;
        private final Map<LambdaSharingMethodVisitor.MethodReferenceId, String> methodReferenceIdToGeneratedFieldName =
                new HashMap<>();
        private final Map<LambdaSharingMethodVisitor.LambdaId, String> lambdaIdToGeneratedFieldName = new HashMap<>();

        protected LambdaSharingClassVisitor(ClassVisitor classVisitor, String className,
                Map<String, String> methodIdToCanonicalMethodId,
                Map<String, LambdaSharingMethodVisitor.InvokeDynamicArgs> generatedFieldNameToInvokeDynamicArgs) {
            super(Opcodes.ASM9, classVisitor);
            this.methodIdToCanonicalMethodId = methodIdToCanonicalMethodId;
            this.generatedFieldNameToInvokeDynamicArgs = generatedFieldNameToInvokeDynamicArgs;
            this.classInternalName = className.replace('.', '/');
        }

        @Override
        public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String descriptor,
                final String signature,
                final String[] exceptions) {
            return new LambdaSharingMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions),
                    classInternalName, methodIdToCanonicalMethodId, methodReferenceIdToGeneratedFieldName,
                    lambdaIdToGeneratedFieldName, generatedFieldNameToInvokeDynamicArgs);
        }
    }

    private static class CreateSharedLambdaFieldsClassVisitor extends ClassVisitor {
        private final String classInternalName;
        private final Map<String, LambdaSharingMethodVisitor.InvokeDynamicArgs> generatedFieldNameToInvokeDynamicArgs;
        private boolean hasClinit = false;

        protected CreateSharedLambdaFieldsClassVisitor(ClassVisitor classVisitor, String className,
                Map<String, LambdaSharingMethodVisitor.InvokeDynamicArgs> generatedFieldNameToInvokeDynamicArgs) {
            super(Opcodes.ASM9, classVisitor);
            this.generatedFieldNameToInvokeDynamicArgs = generatedFieldNameToInvokeDynamicArgs;
            this.classInternalName = className.replace('.', '/');

            for (Map.Entry<String, LambdaSharingMethodVisitor.InvokeDynamicArgs> generatedFieldAndInitializerEntry : generatedFieldNameToInvokeDynamicArgs
                    .entrySet()) {
                String fieldName = generatedFieldAndInitializerEntry.getKey();
                Type fieldDescriptor = generatedFieldAndInitializerEntry.getValue().getFieldDescriptor();
                classVisitor.visitField(Modifier.PRIVATE | Modifier.STATIC,
                        fieldName, fieldDescriptor.getDescriptor(), null, null);
            }
        }

        @Override
        public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String descriptor,
                final String signature,
                final String[] exceptions) {
            if (name.equals("<clinit>")) {
                hasClinit = true;
                return new SharedLambdaFieldsInitializerMethodVisitor(Opcodes.ASM9,
                        super.visitMethod(access, name, descriptor, signature, exceptions),
                        classInternalName, generatedFieldNameToInvokeDynamicArgs);

            } else {
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }

        @Override
        public void visitEnd() {
            if (!hasClinit) {
                MethodVisitor methodVisitor = visitMethod(Modifier.PUBLIC | Modifier.STATIC, "<clinit>",
                        Type.getMethodDescriptor(Type.VOID_TYPE), null, null);
                methodVisitor.visitCode();
                methodVisitor.visitMaxs(1, 0);
                methodVisitor.visitInsn(Opcodes.RETURN);
                methodVisitor.visitEnd();
            }
            super.visitEnd();
        }
    }
}
