package Tests;

import com.google.protobuf.InvalidProtocolBufferException;

import Impactyn.Contracts.RuntimeService.V1.ImpactynContractsRuntimeServiceV1.ExecuteRequest;
import Impactyn.Contracts.RuntimeService.V1.ImpactynContractsRuntimeServiceV1.ExecuteResponse;

import Impactyn.Contracts.Assistants.V1.ImpactynContractsAssistantsV1.GetAssistantConfigRequest;
import Impactyn.Contracts.Assistants.V1.ImpactynContractsAssistantsV1.GetAssistantConfigResponse;
import Impactyn.Contracts.Assistants.V1.ImpactynContractsAssistantsV1.AssistantConfigPrompt;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AssistantConfigTest extends BaseTest{

    @Test(description = "Verify default config is returned based on location when no ResourceId is provided")
    public void testFetchDefaultConfigBasedOnLocation() throws InvalidProtocolBufferException {

        // 2. Build the Request (Empty ResourceId as per test case)
        GetAssistantConfigRequest configRequest = GetAssistantConfigRequest.newBuilder()
                // .setResourceId(...) is NOT called here
                .build();

        // 3. Wrap in ExecuteRequest
        ExecuteRequest exec = ExecuteRequest.newBuilder()
                .setApiVersion("V1")
                .setNamespace("") // Spec says: NO NAMESPACE PROVIDED
                .setResource("Assistants")
                .setName("")      // Spec says: NO NAME PROVIDED
                .setAction("GetAssistantConfig")
                .setContent(configRequest.toByteString())
                .build();

        // 4. Execute the Call
        ExecuteResponse resp = authenticatedStub.execute(exec);

        // 5. Parse the Response
        GetAssistantConfigResponse configResponse = GetAssistantConfigResponse.parseFrom(resp.getContent());

        // 6. Assertions
        Assert.assertNotNull(configResponse, "Response should not be null");

        // Ensure we got a description (Default config should describe the assistant)
        Assert.assertFalse(configResponse.getDescription().isEmpty(), "Description should not be empty");
        System.out.println("Assistant Description: " + configResponse.getDescription());

        // Ensure we got the "Global" prompts
        Assert.assertTrue(configResponse.getPromptsCount() > 0, "Expected at least one default prompt");

        System.out.println("--- Default Prompts Found ---");
        for (AssistantConfigPrompt prompt : configResponse.getPromptsList()) {
            System.out.println("Title: " + prompt.getTitle());

            // For a default config test, ensure none of these prompts have a ResourceId
            // because they are supposed to be "Global"
            Assert.assertFalse(prompt.hasResourceId(),
                    "Global prompt '" + prompt.getTitle() + "' should not have a ResourceId assigned!");
        }
    }
}
