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
        String HOST = dotenv.get("HOST");
        String CLIENT_VERSION = dotenv.get("CLIENT_VERSION");
        String PORTStr = dotenv.get("PORT");
        int PORT = Integer.parseInt(PORTStr);
        String API_KEY = dotenv.get("API_KEY");
        String IMPACTYN_LOCATION = dotenv.get("IMPACTYN_LOCATION");  // We simulate being in Cairo to see if we get the Egypt-specific default config
        String AUTH_TOKEN = dotenv.get("AUTH_TOKEN");

    @BeforeClass
    public void setup() {

        try {

            // Create Secure Channel
            channel = ManagedChannelBuilder.forAddress(HOST, PORT)
                    .useTransportSecurity() // This enables SSL/TLS
                    .build();

            // Initialize Stub
            blockingStub = RuntimeServiceGrpc.newBlockingStub(channel);

            // Setup Headers
            Metadata headers = new Metadata();
            headers.put(
                    Metadata.Key.of("x-impactyn-client-version", Metadata.ASCII_STRING_MARSHALLER),
                    CLIENT_VERSION
            );

            headers.put(
                    Metadata.Key.of("x-impactyn-location", Metadata.ASCII_STRING_MARSHALLER),
                    IMPACTYN_LOCATION
            );

            headers.put(
                    Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
                    AUTH_TOKEN
            );

            authenticatedStub = blockingStub.withInterceptors(
                    MetadataUtils.newAttachHeadersInterceptor(headers)
            );

        } catch (StatusRuntimeException e) {

            if (e.getStatus().getCode() == Status.Code.PERMISSION_DENIED) {

                System.out.println("Permission denied: Invalid or expired authorization token.");

            }
            else if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {

                System.out.println("UNAUTHENTICATED: Invalid or expired authorization token.");

            }
            else {

                System.out.println("gRPC Error: " + e.getStatus());

            }

        } catch (Exception e) {

            System.out.println("Unexpected Error: " + e.getMessage());
        }
    }

    @AfterClass
        public void teardown() {
            if (channel != null) {
                channel.shutdownNow();
            }
        }

}
