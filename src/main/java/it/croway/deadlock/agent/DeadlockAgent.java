package it.croway.deadlock.agent;

import java.lang.instrument.Instrumentation;

public class DeadlockAgent {

    public static void premain(String agentArgs, Instrumentation inst) {

        String targetClassName = "com.example.TargetClass";
        String targetMethodName = "main";

        if (agentArgs != null) {
            String[] args = agentArgs.split(":");
            targetClassName = args[0];
            if (args.length > 1) {
                targetMethodName = args[1];
            }
        }

        System.out.println("DeadlockAgent: Starting instrumentation for class: " + targetClassName +
                ", method: " + targetMethodName);

        inst.addTransformer(new DeadlockClassTransformer(targetClassName, targetMethodName));
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
