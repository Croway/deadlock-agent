# Deadlock Agent

This project provides a Java agent designed to dynamically instrument Java applications to simulate and identify potential deadlock scenarios. It uses the ASM library to modify the bytecode of specified methods at runtime, injecting logic that can force a deadlock to occur.

This is useful for testing an application's resilience to deadlocks and for understanding how they can be triggered in a controlled environment.

## Usage

To use the Deadlock Agent, you need to attach it to the Java application you want to monitor using the `-javaagent` flag. The agent takes arguments specifying the target class and method to instrument.

**Agent Arguments Format:**

`<fully.qualified.ClassName>:<methodName>`

-   `fully.qualified.ClassName`: The full name of the class containing the method to be instrumented.
-   `methodName`: The name of the method to be instrumented.

### Example

The following command demonstrates how to use the agent to simulate a lock within the `getSharedConnection` method of the `org.springframework.jms.listener.AbstractJmsListeningContainer` class in a Spring JMS application.

```bash
java -javaagent:/Users/fmariani/Repositories/croway/deadlock-agent/target/deadlock-agent-1.0-SNAPSHOT.jar=org.springframework.jms.listener.AbstractJmsListeningContainer:getSharedConnection -jar target/acme-1.0-SNAPSHOT.jar
```

**Command Breakdown:**

-   `java`: The Java runtime command.
-   `-javaagent:/Users/fmariani/Repositories/croway/deadlock-agent/target/deadlock-agent-1.0-SNAPSHOT.jar`: Attaches the deadlock agent to the JVM.
-   `=org.springframework.jms.listener.AbstractJmsListeningContainer:getSharedConnection`: Passes arguments to the agent, specifying that it should instrument the `getSharedConnection` method within the `AbstractJmsListeningContainer` class.
-   `-jar target/acme-1.0-SNAPSHOT.jar`: The target Java application to run (replace with your actual application JAR).
