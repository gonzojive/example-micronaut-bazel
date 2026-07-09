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
