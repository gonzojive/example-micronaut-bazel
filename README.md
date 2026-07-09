# Micronaut + Bazel 9 + Kotlin gRPC Microservice Example

A clean, production-ready template demonstrating how to build a gRPC microservice using **Micronaut**, **Kotlin**, and **Bazel 9** (leveraging Bzlmod module management).

This project showcases how to setup a fast-starting, compile-time dependency-injected Kotlin microservice without using Gradle or Maven.

---

## Technical Overview

### 1. Ahead-of-Time (AoT) Dependency Injection
Unlike traditional frameworks that use runtime reflection to scan packages and instantiate beans, **Micronaut** performs dependency injection at compile time using annotation processors. 
This results in:
- Extremely fast startup times (typically sub-second).
- Zero reflection overhead at runtime.
- Minimal memory footprint.

### 2. Code Generation Flow (gRPC & Micronaut)
Building a gRPC microservice requires a two-step code generation compilation process:
1. **gRPC Code Generation**: Protobuf compiler (`protoc`) generates Java and Kotlin message bindings and service stubs from the `.proto` API contract.
2. **Micronaut DI Code Generation**: The Java compiler (`javac`) runs Micronaut's annotation processors against our service classes to generate factory and bean definition metadata.

### 3. Bazel Compilation Architecture
In a Bazel environment, compiling mixed Kotlin/Java codebases while running annotation processors requires clean target boundaries:
- **`hello_proto` & `hello_java_proto`**: Compiles the protobuf schemas and generates the Java/Kotlin gRPC bindings.
- **`service_java_lib`**: A standard Java library that implements the gRPC service endpoint (`HelloServiceJava.java`). This target runs the Micronaut annotation processors (`BeanDefinitionInjectProcessor`) to generate the bean definitions. By isolating the service in a standard Java target, we bypass classloader and path-resolution complications associated with Kotlin-only annotation processing (`Kapt`) in sandboxed Bazel environments.
- **`app_lib`**: The Kotlin library containing the main application bootstrapper (`App.kt`) and client. It depends on `service_java_lib` and packages everything together.
- **`app` & `client`**: Executable binary targets that start the server and run a test request respectively.

---

## Project Structure

```
├── .bazelrc             # Bazel build configuration and JVM flags
├── BUILD.bazel          # Targets for Proto, Java, Kotlin, and binaries
├── MODULE.bazel         # Bzlmod dependencies (rules_kotlin, rules_java, Micronaut, gRPC)
├── src
│   └── main
│       ├── kotlin
│       │   └── example
│       │       ├── App.kt                # Boots Micronaut gRPC server
│       │       ├── Client.kt             # Client script to test the server
│       │       └── HelloServiceJava.java # Java service implementing gRPC Greeter
│       ├── proto
│       │   └── hello.proto               # gRPC Service contract (Greeter / SayHello)
│       └── resources
│           └── logback.xml               # Logback configuration
```

---

## How to Run

### Step 1: Clone and Build
Ensure you have [Bazel](https://bazel.build/install) installed. Build the entire project:
```bash
bazel build //:app //:client
```

### Step 2: Start the gRPC Server
Start the Micronaut gRPC microservice:
```bash
bazel run //:app
```
*The server will boot in a fraction of a second and start listening on port `50051`:*
```
 __  __ _                                  _
|  \/  (_) ___ _ __ ___  _ __   __ _ _   _| |_
| |\/| | |/ __| '__/ _ \| '_ \ / _` | | | | __|
| |  | | | (__| | | (_) | | | | (_| | |_| | |_
|_|  |_|_|\___|_|  \___/|_| |_|\__,_|\__,_|\__|
INFO  io.micronaut.runtime.Micronaut - Startup completed in 288ms. Server Running: http://localhost:50051
```

### Step 3: Run the Test Client
In a separate terminal, execute the client binary to invoke the service:
```bash
bazel run //:client
```
*Expected Output:*
```
Sending request: Antigravity...
Received response: Hello Antigravity!
```
