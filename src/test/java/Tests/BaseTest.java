package Tests;

import io.github.cdimascio.dotenv.Dotenv;

import Impactyn.Contracts.RuntimeService.V1.RuntimeServiceGrpc;

import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public abstract class BaseTest {

        protected ManagedChannel channel;
        protected RuntimeServiceGrpc.RuntimeServiceBlockingStub blockingStub;
        protected RuntimeServiceGrpc.RuntimeServiceBlockingStub authenticatedStub;


        Dotenv dotenv = Dotenv.load();
        String AUTH_TOKEN = dotenv.get("AUTH_TOKEN");
        String HOST = dotenv.get("HOST");
        String CLIENT_VERSION = dotenv.get("CLIENT_VERSION");
        String PORTStr = dotenv.get("PORT");
        int PORT = Integer.parseInt(PORTStr);
        String IMPACTYN_LOCATION = dotenv.get("IMPACTYN_LOCATION");  // We simulate being in Cairo to see if we get the Egypt-specific default config

    @BeforeClass
       public void setup() {
            // 1. Create a Secure Channel
            channel = ManagedChannelBuilder.forAddress(HOST, PORT)
                    .useTransportSecurity() // This enables SSL/TLS
                    .build();

            // 2. Initialize the Stub
            blockingStub = RuntimeServiceGrpc.newBlockingStub(channel);
       }

    @BeforeClass
       public void setupHeaders() {
            Metadata headers = new Metadata();
            headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), AUTH_TOKEN);
            headers.put(Metadata.Key.of("x-impactyn-client-version", Metadata.ASCII_STRING_MARSHALLER), CLIENT_VERSION);
            headers.put(Metadata.Key.of("x-impactyn-location", Metadata.ASCII_STRING_MARSHALLER), IMPACTYN_LOCATION);
           authenticatedStub = blockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
       }

    @AfterClass
        public void teardown() {
            if (channel != null) {
                channel.shutdownNow();
            }
        }

}
