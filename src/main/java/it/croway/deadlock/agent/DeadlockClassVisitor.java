package it.croway.deadlock.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class DeadlockClassVisitor extends ClassVisitor {

    private String className;
    private String targetMethodName;
    private boolean fieldsAdded = false;
    private boolean methodAdded = false;

    public DeadlockClassVisitor(ClassVisitor cv, String targetMethodName) {
        super(Opcodes.ASM9, cv);
        this.targetMethodName = targetMethodName;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        // Add lock fields if not already added
        if (!fieldsAdded) {
            addLockFields();
        }

        // Add deadlock demonstration method
        if (!methodAdded) {
            addDeadlockMethod();
        }

        super.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        // Instrument the specific target method
        if (name.equals(targetMethodName)) {
            System.out.println("DeadlockAgent: Found target method: " + name);
            mv = new DeadlockMethodVisitor(mv, name, className.replace(".", "/"));
        }

        return mv;
    }

    private void addLockFields() {
        // Add private static final Object lock1 = new Object();
        cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "lock1", "Ljava/lang/Object;", null, null);

        // Add private static final Object lock2 = new Object();
        cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "lock2", "Ljava/lang/Object;", null, null);

        fieldsAdded = true;
    }

    private void addDeadlockMethod() {
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "demonstrateDeadlock", "()V", null, null);
        mv.visitCode();

        // Start try block
        Label startTryLabel = new Label();
        Label endTryLabel = new Label();
        Label handlerTryLabel = new Label();
        Label endLabel = new Label();
        mv.visitTryCatchBlock(startTryLabel, endTryLabel, handlerTryLabel, "java/lang/Exception");

        mv.visitLabel(startTryLabel);
        // Create Thread1 with lambda
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/Thread");
        mv.visitInsn(Opcodes.DUP);

        // Create Runnable lambda for Thread1
        mv.visitInvokeDynamicInsn("run", "()Ljava/lang/Runnable;",
                new org.objectweb.asm.Handle(Opcodes.H_INVOKESTATIC,
                        "java/lang/invoke/LambdaMetafactory", "metafactory",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                        false),
                org.objectweb.asm.Type.getType("()V"),
                new org.objectweb.asm.Handle(Opcodes.H_INVOKESTATIC, className, "thread1Logic", "()V", false),
                org.objectweb.asm.Type.getType("()V"));

        mv.visitLdcInsn("Thread1");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Thread", "<init>",
                "(Ljava/lang/Runnable;Ljava/lang/String;)V", false);

        // Store thread1 reference
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        // Start thread1
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "start", "()V", false);

        // Thread.sleep(500)
        mv.visitLdcInsn(500L);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);

        // Synchronized block on lock2
        mv.visitFieldInsn(Opcodes.GETSTATIC, className, "lock2", "Ljava/lang/Object;");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitInsn(Opcodes.MONITORENTER);

        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("Thread acquired lock in class: " + className + ", method: " + targetMethodName);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false);

        // Thread.sleep(1000)
        mv.visitLdcInsn(1000L);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);

        // Synchronized block on lock1
        mv.visitFieldInsn(Opcodes.GETSTATIC, className, "lock1", "Ljava/lang/Object;");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitInsn(Opcodes.MONITORENTER);

        // System.out.println("Thread2 acquired lock1")
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("Thread2 acquired lock1");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false);

        // Exit synchronized blocks
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitInsn(Opcodes.MONITOREXIT);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.MONITOREXIT);

        mv.visitLabel(endTryLabel);
        // Jump to end
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);

        // Exception handler
        mv.visitLabel(handlerTryLabel);
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);

        // End label
        mv.visitLabel(endLabel);
        mv.visitInsn(Opcodes.RETURN);

        mv.visitMaxs(5, 5);
        mv.visitEnd();

        // Add thread1Logic method
        addThread1Logic();

        // Add static initializer for lock fields
        addStaticInitializer();

        methodAdded = true;
    }

    private void addThread1Logic() {
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "thread1Logic", "()V", null, null);
        mv.visitCode();

        // Synchronized block on lock1
        mv.visitFieldInsn(Opcodes.GETSTATIC, className, "lock1", "Ljava/lang/Object;");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, 0);
        mv.visitInsn(Opcodes.MONITORENTER);

        // System.out.println("Thread1 acquired lock1")
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("Thread1 acquired lock1");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false);

        // Try-catch for Thread.sleep
        org.objectweb.asm.Label tryStart = new org.objectweb.asm.Label();
        org.objectweb.asm.Label tryEnd = new org.objectweb.asm.Label();
        org.objectweb.asm.Label catchStart = new org.objectweb.asm.Label();
        org.objectweb.asm.Label finallyStart = new org.objectweb.asm.Label();

        mv.visitTryCatchBlock(tryStart, tryEnd, catchStart, "java/lang/InterruptedException");

        mv.visitLabel(tryStart);
        // Thread.sleep(100000)
        mv.visitLdcInsn(100000L);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);

        // Synchronized block on lock2
        mv.visitFieldInsn(Opcodes.GETSTATIC, className, "lock2", "Ljava/lang/Object;");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        mv.visitInsn(Opcodes.MONITORENTER);

        // System.out.println("Thread1 acquired lock2")
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("Thread1 acquired lock2");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.MONITOREXIT);

        mv.visitLabel(tryEnd);
        mv.visitJumpInsn(Opcodes.GOTO, finallyStart);

        // Catch InterruptedException
        mv.visitLabel(catchStart);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread",
                "()Ljava/lang/Thread;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "interrupt", "()V", false);

        mv.visitLabel(finallyStart);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.MONITOREXIT);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    private void addStaticInitializer() {
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();

        // Initialize lock1
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/Object");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, className, "lock1", "Ljava/lang/Object;");

        // Initialize lock2
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/Object");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, className, "lock2", "Ljava/lang/Object;");

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 0);
        mv.visitEnd();
    }
}
