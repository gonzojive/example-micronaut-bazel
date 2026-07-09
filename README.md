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

## Code Walkthrough (For Micronaut Beginners)

Let's walk through the core parts of the microservice.

### 1. The API Contract: `src/main/proto/hello.proto`
gRPC services are defined using Protocol Buffers. We define a simple `Greeter` service with a `SayHello` method:

```proto
syntax = "proto3";

option java_multiple_files = true;
option java_package = "example";
option java_outer_classname = "HelloWorldProto";

package example;

// The greeting service definition.
service Greeter {
  // Sends a greeting
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

// The request message containing the user's name.
message HelloRequest {
  string name = 1;
}

// The response message containing the greetings
message HelloReply {
  string message = 1;
}
```

### 2. The Service Implementation: `src/main/kotlin/example/HelloServiceJava.java`
This class implements the generated `GreeterGrpc.GreeterImplBase` stub interface. We annotate it with `@GrpcService` to register it as a gRPC endpoint:

```java
package example;

import io.grpc.stub.StreamObserver;
import io.micronaut.grpc.annotation.GrpcService;
import jakarta.inject.Singleton;

@GrpcService 
@Singleton
public class HelloServiceJava extends GreeterGrpc.GreeterImplBase {
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String name = request.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "World";
        }
        HelloReply reply = HelloReply.newBuilder()
            .setMessage("Hello " + name + "!")
            .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
```
*   `@GrpcService`: A specialized Micronaut stereotype annotation that inherits from `@Singleton`. It exposes this bean to Micronaut's gRPC server engine so that the server knows to route matching gRPC requests to this class.
*   `StreamObserver`: The asynchronous callback receiver used by gRPC java stubs to stream response messages back to the client.

### 3. Bootstrapping the Server: `src/main/kotlin/example/App.kt`
Starting the microservice is extremely simple. We call the Micronaut bootstrapper to scan our package and startup the Netty server:

```kotlin
package example

import io.micronaut.runtime.Micronaut

fun main(args: Array<String>) {
    Micronaut.build()
        .args(*args)
        .packages("example")
        .start()
}
```
*   `Micronaut.build()`: Configures the boot runtime context.
*   `.packages("example")`: Restricts package scanning to the specified root package to keep DI startup as fast as possible.
*   `.start()`: Starts the dependency injection context and fires up the gRPC Netty server on port `50051`.

### 4. Testing with the Client: `src/main/kotlin/example/Client.kt`
We also provide a client script that uses Kotlin Coroutines to asynchronously query the server:

```kotlin
package example

import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking

fun main() {
    val channel = ManagedChannelBuilder.forAddress("localhost", 50051)
        .usePlaintext()
        .build()

    val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

    runBlocking {
        val request = HelloRequest.newBuilder().setName("Antigravity").build()
        println("Sending request: ${request.name}...")
        val response = stub.sayHello(request)
        println("Received response: ${response.message}")
    }

    channel.shutdown()
}
```
*   `ManagedChannelBuilder`: Establishes the TCP connection to the gRPC server.
*   `GreeterCoroutineStub`: The coroutine-based client stub compiled by `grpc_kotlin` from the protobuf schema. It allows you to write non-blocking asynchronous calls in a clean, synchronous-looking style.

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
