package Tests;
import Impactyn.Contracts.RuntimeService.V1.RuntimeServiceGrpc;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public abstract class BaseTest {

        protected  ManagedChannel channel;
        protected RuntimeServiceGrpc.RuntimeServiceBlockingStub blockingStub;
        protected RuntimeServiceGrpc.RuntimeServiceBlockingStub authenticatedStub;

        private final String HOST = "staging.impactyn.io";
        private final int PORT = 530;
        final String AUTH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJJbXBhY3R5bi5DcmVkZW50aWFsIjoidHlwZS9jcmVkZW50aWFscy9ucy9nb29nbGUvbmFtZS8xMTc0MzYzODU3NzkxNzU3MjQ3MzIiLCJJbXBhY3R5bi5EZXZpY2VJZCI6IjM2Nzk3NjY3LTI5NTMtYTE0OC03NGZiLWVmMjA5M2E5YTdmZSIsIkltcGFjdHluLkRldmljZU5hbWUiOiIiLCJJbXBhY3R5bi5JZGVudGl0eSI6InR5cGUvaWRlbnRpdGllcy9ucy8wZDQ0NjNlOTlhMDY0ZDg1MjMyODg5OGFlOTViYTU5Yl84NWNjYTdmYmQ4NDY2Mzk3L25hbWUvZGVmYXVsdCIsIkltcGFjdHluLlRva2VuVHlwZSI6ImlkZW50aXR5LmFjY2VzcyIsIkltcGFjdHluLlJvbGUiOlsiQ2dad2RXSnNhV01hREFpM2l1RFFCaEM0aWVMakFnPT0iLCJDZ1IxYzJWeUVqRXdaRFEwTmpObE9UbGhNRFkwWkRnMU1qTXlPRGc1T0dGbE9UVmlZVFU1WWw4NE5XTmpZVGRtWW1RNE5EWTJNemszR2d3SXQ0cmcwQVlRdUluaTR3ST0iXSwibmJmIjoxNzc3MzY3MDk1LCJleHAiOjE3Nzk5NTkwOTUsImlhdCI6MTc3NzM2NzA5NSwiaXNzIjoiSW1wYWN0eW4uSWRlbnRpdHkiLCJhdWQiOiJJbXBhY3R5bi5Gcm9udGRvb3IifQ.fZTvT-pq_bEzm5OX6u_UkbjS_7cafrsUxo8C0vGXQls";
        final String INVALID_TOKEN = "FJGJSLHKGDGIKJ";
        final String CLIENT_VERSION = "impactyn.test/2.0.25";

        @BeforeClass
        public void setup() {
            // 1. Create a Secure Channel (Equivalent to grpc.ssl_channel_credentials)
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
           // We simulate being in Cairo to see if we get the Egypt-specific default config
           headers.put(Metadata.Key.of("x-impactyn-location", Metadata.ASCII_STRING_MARSHALLER), "30.0444,31.2357");
           authenticatedStub = blockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
       }

        @AfterClass
        public void teardown() {
            if (channel != null) {
                channel.shutdownNow();
            }
        }

        void logger()
        {


        }

}
