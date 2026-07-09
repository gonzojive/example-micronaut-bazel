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
