package Tests;
import io.grpc.*;

import com.google.protobuf.InvalidProtocolBufferException;

import Impactyn.Contracts.RuntimeService.V1.ImpactynContractsRuntimeServiceV1.GetRequest;
import Impactyn.Contracts.RuntimeService.V1.ImpactynContractsRuntimeServiceV1.GetResponse;
import Impactyn.Contracts.FeedTemplate.V1.ImpactynContractsFeedTemplateV1.*;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FeedTemplatesTest extends BaseTest {

    public FeedTemplatesTest(){
        super();
    }

    @DataProvider(name = "feedDataProvider")
    public Object[][] loadTestData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(new File("src/main/resources/data/feed_expectations.json"));

        Object[][] data = new Object[rootNode.size()][2];
        int i = 0;
        Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            data[i][0] = entry.getKey();   // feed_key
            data[i][1] = entry.getValue(); // test_data node
            i++;
        }
        return data;
    }

    @Test(dataProvider = "feedDataProvider")
    public void testFeedTemplates(String feedKey, JsonNode testData) throws InvalidProtocolBufferException {
        JsonNode requestData = testData.get("request");
        JsonNode expectations = testData.get("expectations");

        // 2. Create Request
        GetRequest request = GetRequest.newBuilder()
                .setApiVersion("V1")
                .setNamespace(requestData.get("namespace").asText())
                .setResource(requestData.get("resource").asText())
                .setName(requestData.get("name").asText())
                .setView(requestData.get("view").asText())
                .build();

        // 3. Handle Expected SUCCESS vs FAILURE
        if (expectations.get("expected_status").asText().equals("OK")) {

            GetResponse response = authenticatedStub.get(request);
            Assert.assertNotNull(response);

            // Parse Home (GetFeedResponse)
            GetFeedResponse home = GetFeedResponse.parseFrom(response.getContent());
            Feed feed = home.getFeed();

            // Assert number of sections
            Assert.assertTrue(feed.getItemsCount() > expectations.get("min_sections").asInt(),
                    "Found " + feed.getItemsCount() + " sections");

            // 4. Loop through Sections
            for (int i = 0; i < feed.getItemsCount(); i++) {
                FeedItem sectionItem = feed.getItems(i);

                Assert.assertEquals(sectionItem.getType(), "section");
                Assert.assertFalse(sectionItem.getPropertyBag().isEmpty(), "Section " + i + " property bag is empty");

                SectionItemProperties sectionProps = SectionItemProperties.parseFrom(sectionItem.getPropertyBag());
                System.out.println("\nSECTION " + (i + 1) + ": " + sectionProps.getTitle());

                // 5. Loop through nested items in section
                for (int j = 0; j < sectionProps.getItemsCount(); j++) {
                    FeedItem subItem = sectionProps.getItems(j);
                    String type = subItem.getType();
                    System.out.println("     item [" + j + "] Type: " + type);

                    // Check allowed types
                    boolean isAllowed = false;
                    for (JsonNode allowed : expectations.get("allowed_item_types")) {
                        if (allowed.asText().equals(type)) { isAllowed = true; break; }
                    }
                    Assert.assertTrue(isAllowed, "Type " + type + " is not allowed");

                    // 6. Dynamic Parsing based on Type
                    parsePropertyBagByType(type, subItem);
                }
            }
        } else {
            // NEGATIVE TEST
            StatusRuntimeException exception = Assert.expectThrows(StatusRuntimeException.class, () -> authenticatedStub.get(request));
            Assert.assertEquals(exception.getStatus().getCode(), Status.Code.UNAUTHENTICATED);
            System.out.println("Confirmed: Access denied for " + feedKey);
        }
    }

    private void parsePropertyBagByType(String type, FeedItem item) throws InvalidProtocolBufferException {
        switch (type) {
            case "hero":
                HeroItemProperties hero = HeroItemProperties.parseFrom(item.getPropertyBag());
                System.out.println("        >> Hero Title: " + hero.getTitle());
                break;
            case "banner":
                BannerItemProperties banner = BannerItemProperties.parseFrom(item.getPropertyBag());
                System.out.println("        >> Banner Badge: " + banner.getBadgeText());
                break;
            case "category":
                CategoryItemProperties cat = CategoryItemProperties.parseFrom(item.getPropertyBag());
                System.out.println("        >> Category Title: " + cat.getTitle());
                break;
            case "cover":
                CoverItemProperties cover = CoverItemProperties.parseFrom(item.getPropertyBag());
                System.out.println("        >> Cover Title: " + cover.getTitle());
                break;
            case "shop":
                ShopItemProperties shop = ShopItemProperties.parseFrom(item.getPropertyBag());
                System.out.println("        >> Shop Title: " + shop.getTitle());
                break;
            case "user":
                UserItemProperties user = UserItemProperties.parseFrom(item.getPropertyBag());
                System.out.println("        >> User Title: " + user.getTitle());
                break;
            case "review":
                ReviewItemProperties review = ReviewItemProperties.parseFrom(item.getPropertyBag());
                System.out.println("        >> Review Title: " + review.getTitle());
                break;
        }
    }

}
