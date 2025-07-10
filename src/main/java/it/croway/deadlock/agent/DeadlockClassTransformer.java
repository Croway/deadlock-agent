package it.croway.deadlock.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;

class DeadlockClassTransformer implements ClassFileTransformer {

    private final String targetClassName;
    private final String targetMethodName;

    public DeadlockClassTransformer(String targetClassName, String targetMethodName) {
        this.targetClassName = targetClassName.replace('.', '/');
        this.targetMethodName = targetMethodName;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        if (className.equals(targetClassName)) {
            System.out.println("DeadlockAgent: Transforming class: " + className);

            try {
                ClassReader classReader = new ClassReader(classfileBuffer);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                ClassVisitor classVisitor = new DeadlockClassVisitor(classWriter, targetMethodName);

                classReader.accept(classVisitor, 0);

                return classWriter.toByteArray();
            } catch (Exception e) {
                System.err.println("DeadlockAgent: Error transforming class: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return null;
    }
}
