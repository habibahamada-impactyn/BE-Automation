package Tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;

import Impactyn.Contracts.RuntimeService.V1.ImpactynContractsRuntimeServiceV1.ExecuteRequest;
import Impactyn.Contracts.RuntimeService.V1.ImpactynContractsRuntimeServiceV1.ExecuteResponse;

import Impactyn.Contracts.RuntimeService.V1.ImpactynContractsRuntimeServiceV1.GetRequest;
import Impactyn.Contracts.RuntimeService.V1.ImpactynContractsRuntimeServiceV1.GetResponse;

import Impactyn.Contracts.Assistants.V1.ImpactynContractsAssistantsV1;
import Impactyn.Contracts.Common.V1.ImpactynContractsCommonV1;

import Impactyn.Contracts.Assistants.V1.ImpactynContractsAssistantsV1.AskRequest;
import Impactyn.Contracts.Assistants.V1.ImpactynContractsAssistantsV1.AskResponse;
import Impactyn.Contracts.Assistants.V1.ImpactynContractsAssistantsV1.Message;

import Impactyn.Contracts.Assistants.V1.ImpactynContractsAssistantsV1.GetChatListResponse;
import Impactyn.Contracts.Assistants.V1.ImpactynContractsAssistantsV1.Chat;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class ChatAssistantTest extends BaseTest {

    public ChatAssistantTest(){
        super();
    }

    /**
     * Helper to wrap AskRequest into ExecuteRequest and parse the result
     */
    private AskResponse sendAskRequest(AskRequest askPayload)  {
        try {
            ExecuteRequest exec = ExecuteRequest.newBuilder()
                    .setApiVersion("V1")
                    .setNamespace("0d4463e99a064d852328898ae95ba59b_85cca7fbd8466397")
                    .setResource("Assistants")
                    .setAction("Ask")
                    .setContent(askPayload.toByteString())
                    .build();
            ExecuteResponse resp = authenticatedStub.execute(exec);
            return AskResponse.parseFrom(resp.getContent());
        }
       catch (StatusRuntimeException e) {

          if (e.getStatus().getCode() == Status.Code.PERMISSION_DENIED) {

            System.out.println("Permission denied: You are not authorized to access this resource.");

          } else if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {

              System.out.println("UNAUTHENTICATED: You are not authorized to access this resource.");

          } else
          {

            System.out.println("gRPC Error: " + e.getStatus());
          }

       } catch (InvalidProtocolBufferException e) {

         System.out.println("Failed to parse response: " + e.getMessage());

       } catch (Exception e) {

         System.out.println("Unexpected Error: " + e.getMessage());
       }

        return null;
   }

    private long getLastMessageId(AskResponse response) {
        if (response.getThreadCount() == 0) {
            throw new RuntimeException("The thread is empty! No messages found.");
        }
        // Returns the ID of the last element in the list
        return response.getThread(response.getThreadCount() - 1).getId();
    }

    private ImpactynContractsCommonV1.ResourceId createNewChat()  {
        try {
            // 1. Build an empty CreateChatRequest
            ImpactynContractsAssistantsV1.CreateChatRequest createReq =
                    ImpactynContractsAssistantsV1.CreateChatRequest.newBuilder().build();
            // 2. Wrap it in Execute Request
            ExecuteRequest exec = ExecuteRequest.newBuilder()
                    .setApiVersion("V1")
                    .setNamespace("0d4463e99a064d852328898ae95ba59b_85cca7fbd8466397")
                    .setResource("Assistants")
                    .setName("")
                    .setAction("Create")
                    .setContent(createReq.toByteString())
                    .build();

            // 3. Execute and Parse
            ExecuteResponse resp = authenticatedStub.execute(exec);
            ImpactynContractsAssistantsV1.CreateChatResponse createResp = ImpactynContractsAssistantsV1.CreateChatResponse.parseFrom(resp.getContent());

            System.out.println("New Chat Created successfully. ID: " + createResp.getChatId().getName());

            // 4. Return the server-generated ChatId
            return createResp.getChatId();

        } catch (StatusRuntimeException e) {

            if (e.getStatus().getCode() == Status.Code.PERMISSION_DENIED) {

                System.out.println(
                        "Permission denied: You are not authorized to create a new chat."
                );

            }  else if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {

                System.out.println("UNAUTHENTICATED: You are not authorized to create a new chat.");

            } else {

                System.out.println("gRPC Error: " + e.getStatus());
            }

        } catch (InvalidProtocolBufferException e) {

            System.out.println(
                    "Failed to parse CreateChat response: " + e.getMessage()
            );

        } catch (Exception e) {

            System.out.println("Unexpected Error: " + e.getMessage());
        }

        return null;
    }

    @DataProvider(name = "ChatAssistantDataProvider")
    public Object[][] loadTestData() throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(new File("src/main/resources/data/ChatAssistantData.json"));

        // Prepare the 2D array for TestNG
        Object[][] data = new Object[rootNode.size()][2];

        for (int i = 0; i < rootNode.size(); i++) {
            // We pass the whole JsonNode
            data[i][0] = rootNode.get(i).get("MessageToSent").asText();
            data[i][1] = rootNode.get(i).get("indexFromLastMessageId").asInt();
        }

        return data;
    }

    @Test(dataProvider = "ChatAssistantDataProvider",description = "Verify that sending a message without LastMessageId returns new message and the full thread history")
    public void testAskNoLastMessageId(String messageToSent, int indexFromLastMessageId)  {

        System.out.println("Verify that sending a message without LastMessageId returns new message and the full thread history");
        AskRequest payload = AskRequest.newBuilder()
                .setMessage(messageToSent)
                .build();

        AskResponse response = sendAskRequest(payload);

        // Assert : That the number of Threads is at least equal to one
        Assert.assertNotNull(response,"Expected number of threads no to be null.");
        Assert.assertTrue(response.getThreadCount() >= 1);
        System.out.println("Full thread count: " + response.getThreadCount());

        for (int i = 0; i < response.getThreadCount(); i++) {
            var message = response.getThread(i);
            String role = message.getRole(); // "you" or "impactyn"
            String textContent = "";

            // Determine which content to pull based on the role
            if ("you".equalsIgnoreCase(role)) {
                textContent = message.getUser().getText().getMessage();
            } else if ("impactyn".equalsIgnoreCase(role)) {
                textContent = message.getAssistant().getText().getMessage();
            }

            // Print in a readable format
            System.out.printf("[%d] %-10s: %s%n",
                    message.getId(),
                    role.toUpperCase(),
                    textContent);

        }
    }

    @Test(dataProvider = "ChatAssistantDataProvider",description = "Verify that sending LastMessageId returns only messages newer than that ID")
    public void testAskWithLastMessageId(String messageToSent, int indexFromLastMessageId)  {
        System.out.println("Verify that sending LastMessageId returns only messages newer than that ID");

        // 1. Get current thread to find an ID
        AskResponse initialResp = sendAskRequest(AskRequest.newBuilder().setMessage(messageToSent).build());
        Assert.assertNotNull(initialResp,"Expected current thread no to be null.");
        long LastMessageId = getLastMessageId(initialResp);

        long OurAnchorID = LastMessageId-indexFromLastMessageId;

        // 2. Request with LastMessageId
        AskRequest payload = AskRequest.newBuilder()
                .setMessage("Show me only new restaurants")
                .setLastMessageId(OurAnchorID)
                .build();

        AskResponse response = sendAskRequest(payload);

        Assert.assertNotNull(response,"Expected number of threads no to be null.");
        System.out.println("Messages received after cutoff: " + response.getThreadCount());

        // Assert: Every message ID in the response must be greater than middleId
        for (Message msg : response.getThreadList()) {
            System.out.println("Checking Message ID: " + msg.getId() + " [Role: " + msg.getRole() + "]");

            // This is the actual validation
            Assert.assertTrue(msg.getId() > OurAnchorID,
                    "FAIL: Received message ID " + msg.getId() + " which is not newer than Our Anchor Id:" + OurAnchorID);

            // Optional: Print the text too
            String text = "you".equalsIgnoreCase(msg.getRole())
                    ? msg.getUser().getText().getMessage()
                    : msg.getAssistant().getText().getMessage();
            System.out.println("  Content: " + text);
        }
    }

    @Test(dataProvider = "ChatAssistantDataProvider",description = "Verify message is appended to default chat when no ChatId is provided")
    public void testAskNoChatId(String messageToSent, int indexFromLastMessageId)  {
        AskRequest payload = AskRequest.newBuilder()
                .setMessage(messageToSent)
                .build();

        AskResponse response = sendAskRequest(payload);
        Assert.assertNotNull(response,"response shouldn't be null.");
        // Note: Success here implies server handled default chat logic
    }

    @Test(dataProvider = "ChatAssistantDataProvider",description = "Verify message is appended to a specific chat when ChatId is provided")
    public void testAskWithSpecificChatId(String messageToSent, int indexFromLastMessageId)  {
        try{

            ImpactynContractsCommonV1.ResourceId chatId = createNewChat();

            Assert.assertNotNull(
                    chatId,
                    "Chat ID is null. Cannot proceed with Ask request."
            );

            AskRequest payload = AskRequest.newBuilder()
                    .setMessage(messageToSent)
                    .setChatId(chatId)
                    .build();

            AskResponse response = sendAskRequest(payload);
            Assert.assertNotNull(
                    response,
                    "AskResponse is null."
            );
            // Assert : That the number of Threads is at least equal to one
            Assert.assertTrue(response.getThreadCount() >= 1);
            System.out.println("Full thread count: " + response.getThreadCount());
        } catch (Exception e) {

        System.out.println("Unexpected Error: " + e.getMessage());

        Assert.fail("Unexpected error occurred.");
        }
    }

    @Test(description = "Verify context-aware response using ResourceId (e.g. searching within a specific brand)")
    public void testAskWithContextResourceId()  {
        // Resource ID representing "BRGR" brand
        ImpactynContractsCommonV1.ResourceId brgrContext = ImpactynContractsCommonV1.ResourceId.newBuilder()
                .setNamespace("brgr.eg")
                .setName("default")
                .setResourceType("metaobjects")
                .build();

        AskRequest payload = AskRequest.newBuilder()
                .setMessage("List items")
                .setResourceId(brgrContext)
                .build();

        AskResponse response = sendAskRequest(payload);

        // Logic check: The response should ideally only contain info related to BRGR
        Assert.assertNotNull(response,"Expected number of threads no to be null.");
        String assistantText = response.getThread(response.getThreadCount() - 1)
                .getAssistant().getText().getMessage();

        System.out.println("Context Response: " + assistantText);
        // Assert that the response contains data or context related to the resource ID sent
        Assert.assertFalse(assistantText.isEmpty());
    }

    @Test(description = "Verify that all chats in the list have a valid title")
    public void testFetchChatsAndVerifyTitles()  {

        try {
            // 2. Build the GetRequest (Using 'GetList' as the view)
            GetRequest getRequest = GetRequest.newBuilder()
                    .setApiVersion("V1")
                    .setNamespace("0d4463e99a064d852328898ae95ba59b_85cca7fbd8466397")
                    .setResource("Assistants")
                    .setName("")
                    .setView("GetList")
                    .build();

            // 3. Call the API
            GetResponse response = authenticatedStub.get(getRequest);

            // 4. Parse the content into GetChatListResponse
            GetChatListResponse chatList = GetChatListResponse.parseFrom(response.getContent());

            // 5. Assertions & Proof
            System.out.println("Total Chats found: " + chatList.getChatsCount());

            for (Chat chat : chatList.getChatsList()) {
                System.out.println("Checking Chat ID: " + chat.getId().getName());
                System.out.println("Chat Title: " + chat.getTitle());

                // THE TEST CASE REQUIREMENT: Ensure chats have a title
                Assert.assertFalse(chat.getTitle().isEmpty(),
                        "Chat with ID " + chat.getId().getName() + " has an empty title!");

                Assert.assertNotNull(chat.getTitle(), "Title should not be null.");
            }
    } catch (StatusRuntimeException e) {

        if (e.getStatus().getCode() == Status.Code.PERMISSION_DENIED) {

        System.out.println(
                    "Permission denied: You are not authorized to fetch chats.");
        Assert.fail("Permission denied while fetching chats.");

        } else  if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {

            System.out.println(
                    "Permission denied: You are not authorized to fetch chats.");
            Assert.fail("UNAUTHENTICATED while fetching chats.");
        } else {
            System.out.println("gRPC Error: " + e.getStatus());
            Assert.fail("gRPC Error occurred: " + e.getStatus());
        }
    } catch (InvalidProtocolBufferException e) {

        System.out.println(
                "Failed to parse GetChatList response: "
                        + e.getMessage()
        );

        Assert.fail("Response parsing failed.");

    } catch (Exception e) {

        System.out.println("Unexpected Error: " + e.getMessage());

        Assert.fail("Unexpected error occurred.");
    }

    }
}
