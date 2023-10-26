package ai.timefold.solver.quarkus.deployment;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

/**
 * This {@link MethodVisitor} creates temporarily invalid bytecode; It converts lambdas/method references to fields reads, but
 * does not create/initialize fields. The reason is we need to initialize the fields in clinit, but clinit itself need to be
 * visited, and we do not know the fields or their initializers until visitEnd of this visitor is called.
 */
public class LambdaSharingMethodVisitor extends InstructionAdapter {
    public record FunctionInterfaceId(String classDescriptor, Type methodGenericType) {
    }

    public record MethodReferenceId(FunctionInterfaceId functionInterfaceId, Handle methodHandle) {
    }

    public record LambdaId(FunctionInterfaceId functionInterfaceId, String methodId) {
    }

    public record InvokeDynamicArgs(String name, String descriptor, Handle bootstrapMethodHandle,
            Object[] bootstrapMethodArguments) {
        public Type getFieldDescriptor() {
            return Type.getReturnType(descriptor);
        }
    }

    final Map<String, String> methodIdToCanonicalMethodId;
    final Map<MethodReferenceId, String> methodReferenceIdToGeneratedFieldName;
    final Map<LambdaId, String> lambdaIdToGeneratedFieldName;
    final Map<String, InvokeDynamicArgs> generatedFieldNameToInvokeDynamicArgs;
    final String classInternalName;

    public LambdaSharingMethodVisitor(MethodVisitor methodVisitor, String classInternalName,
            Map<String, String> methodIdToCanonicalMethodId,
            Map<MethodReferenceId, String> methodReferenceIdToGeneratedFieldName,
            Map<LambdaId, String> lambdaIdToGeneratedFieldName,
            Map<String, InvokeDynamicArgs> generatedFieldNameToInvokeDynamicArgs) {
        super(Opcodes.ASM9, methodVisitor);
        this.classInternalName = classInternalName;
        this.methodIdToCanonicalMethodId = methodIdToCanonicalMethodId;
        this.methodReferenceIdToGeneratedFieldName = methodReferenceIdToGeneratedFieldName;
        this.lambdaIdToGeneratedFieldName = lambdaIdToGeneratedFieldName;
        this.generatedFieldNameToInvokeDynamicArgs = generatedFieldNameToInvokeDynamicArgs;
    }

    protected static String getMethodId(String name, String descriptor) {
        return name + " " + descriptor;
    }

    protected static String getMethodName(String key) {
        return key.substring(0, key.indexOf(' '));
    }

    protected static String getDescriptor(String key) {
        return key.substring(key.indexOf(' ') + 1);
    }

    @Override
    public void invokedynamic(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object[] bootstrapMethodArguments) {
        if (Type.getMethodType(descriptor).getArgumentTypes().length != 0) {
            // The lambda depends on variables used in the method and thus cannot be transformed
            super.invokedynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            return;
        }
        if (!bootstrapMethodHandle.getOwner().equals(Type.getInternalName(LambdaMetafactory.class))) {
            super.invokedynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            return;
        }
        if (!bootstrapMethodHandle.getName().equals("metafactory")) {
            super.invokedynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            return;
        }
        if (!bootstrapMethodHandle.getDesc().equals(Type.getMethodDescriptor(
                Type.getType(CallSite.class),
                Type.getType(MethodHandles.Lookup.class),
                Type.getType(String.class),
                Type.getType(MethodType.class),
                Type.getType(MethodType.class),
                Type.getType(MethodHandle.class),
                Type.getType(MethodType.class)))) {
            super.invokedynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            return;
        }
        if (bootstrapMethodArguments.length != 3) {
            super.invokedynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            return;
        }
        Handle methodHandle = (Handle) bootstrapMethodArguments[1];
        Type genericSignature = (Type) bootstrapMethodArguments[2];

        if (!methodHandle.getOwner().equals(classInternalName)) {
            replaceMethodReference(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments, methodHandle,
                    genericSignature);
        } else {
            replaceLambda(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments, methodHandle, genericSignature);
        }
    }

    private void replaceMethodReference(final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object[] bootstrapMethodArguments,
            Handle methodHandle,
            Type genericSignature) {
        MethodReferenceId methodReferenceId =
                new MethodReferenceId(new FunctionInterfaceId(descriptor, genericSignature), methodHandle);
        replaceDynamicWithFieldRead(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments,
                methodReferenceIdToGeneratedFieldName, methodReferenceId);
    }

    private void replaceLambda(final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object[] bootstrapMethodArguments,
            Handle methodHandle,
            Type genericSignature) {
        String methodKey = getMethodId(methodHandle.getName(), methodHandle.getDesc());
        String canonicalMethodId = methodIdToCanonicalMethodId.get(methodKey);
        LambdaId lambdaId = new LambdaId(new FunctionInterfaceId(descriptor, genericSignature), canonicalMethodId);
        replaceDynamicWithFieldRead(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments,
                lambdaIdToGeneratedFieldName, lambdaId);
    }

    private <Key_> void replaceDynamicWithFieldRead(final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object[] bootstrapMethodArguments,
            Map<Key_, String> idToGeneratedField,
            Key_ id) {
        Type fieldType = Type.getReturnType(descriptor);
        if (idToGeneratedField.containsKey(id)) {
            String generatedFieldName = idToGeneratedField.get(id);
            this.getstatic(classInternalName, generatedFieldName, fieldType.getDescriptor());
            return;
        }
        int fieldId = generatedFieldNameToInvokeDynamicArgs.size();
        String generatedFieldName = "$timefoldSharedLambda$" + fieldId;
        generatedFieldNameToInvokeDynamicArgs.put(generatedFieldName,
                new InvokeDynamicArgs(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments));
        idToGeneratedField.put(id, generatedFieldName);
        this.getstatic(classInternalName, generatedFieldName, fieldType.getDescriptor());
    }
}
