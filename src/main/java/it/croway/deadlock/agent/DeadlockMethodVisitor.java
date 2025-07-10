package it.croway.deadlock.agent;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class DeadlockMethodVisitor extends MethodVisitor {

    private final String methodName;
    private final String clazzName;
    private boolean injected = false;

    public DeadlockMethodVisitor(MethodVisitor mv, String methodName, String clazzName) {
        super(Opcodes.ASM9, mv);
        this.methodName = methodName;
        this.clazzName = clazzName;
    }

    @Override
    public void visitCode() {
        super.visitCode();

        // Inject call to demonstrateDeadlock at the beginning of the method
        if (!injected) {
            System.out.println("DeadlockAgent: Injecting deadlock call into method: " + methodName);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, clazzName, "demonstrateDeadlock", "()V", false);
            injected = true;
        }
    }
}
