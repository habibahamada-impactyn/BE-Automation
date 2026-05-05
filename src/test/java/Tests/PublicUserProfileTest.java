package Tests;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import Impactyn.Contracts.RuntimeService.V1.RuntimeServiceGrpc;
import Impactyn.Contracts.RuntimeService.V1.ImpactynContractsRuntimeServiceV1.GetRequest;
import Impactyn.Contracts.RuntimeService.V1.ImpactynContractsRuntimeServiceV1.GetResponse;
import Impactyn.Contracts.UserProfiles.V1.ImpactynContractsUserProfilesV1.PublicUserProfile;

import com.google.protobuf.ByteString;

import org.testng.annotations.Test;
import org.testng.Assert;

public class PublicUserProfileTest extends BaseTest {

    public PublicUserProfileTest() {
        super();
    }

    @Test(priority = 0, description = "")
    public void testPublicUserProfile() throws com.google.protobuf.InvalidProtocolBufferException {

        // 3. Create the Request
        GetRequest request = GetRequest.newBuilder()
                .setApiVersion("V1")
                .setNamespace("d8bb8494c5bed03dccdfb6976ca9c6a1_af7b47a2218aab7c")
                .setResource("UserProfiles")
                .setName("default")
                .setView("Public")
                .build();

        // 4. Call the Get method
        GetResponse response = authenticatedStub.get(request);
        ByteString contentBytes = response.getContent();
        PublicUserProfile profile = PublicUserProfile.parseFrom(contentBytes);
        // 6. Assertions
        Assert.assertNotNull(response, "Response should not be null");

        System.out.println("Received " + contentBytes.size() + " bytes of data");
        Assert.assertFalse(contentBytes.isEmpty(), "Content should not be empty");

        // Detailed Assertions
        Assert.assertTrue(profile.getDisplayName().contains("habeba"),
                "Display name mismatch. Got: " + profile.getDisplayName());
        Assert.assertEquals(profile.getFollowers(), 2, "Followers count mismatch");

        // Console Output
        System.out.println("\n--- Decoded Profile Data ---");
        System.out.println("Display Name: " + profile.getDisplayName());
        System.out.println("Bio: " + profile.getBio());
        System.out.println("Followers: " + profile.getFollowers());
    }
}
