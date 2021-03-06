package com.iterable.iterableapi;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class IterableApiRequestsTest {

    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        createIterableApi();
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        server = null;
    }

    private void createIterableApi() {
        IterableApi.sharedInstanceWithApiKey(InstrumentationRegistry.getTargetContext(), "fake_key", "test_email");
    }

    @Test
    public void testTrackPurchase() throws Exception {
        String expectedRequest = "{\"user\":{\"email\":\"test_email\"},\"items\":[{\"id\":\"sku123\",\"name\":\"Item\",\"price\":50,\"quantity\":2}],\"total\":100}";

        CommerceItem item1 = new CommerceItem("sku123", "Item", 50.0, 2);
        List<CommerceItem> items = new ArrayList<CommerceItem>();
        items.add(item1);

        IterableApi.sharedInstance.trackPurchase(100.0, items);

        RecordedRequest request = server.takeRequest();
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, request.getPath());
        assertEquals(expectedRequest, request.getBody().readUtf8());
    }

    @Test
    public void testTrackPurchaseWithDataFields() throws Exception {
        String expectedRequest = "{\"user\":{\"email\":\"test_email\"},\"items\":[{\"id\":\"sku123\",\"name\":\"Item\",\"price\":50,\"quantity\":2}],\"total\":100,\"dataFields\":{\"field\":\"testValue\"}}";

        CommerceItem item1 = new CommerceItem("sku123", "Item", 50.0, 2);
        List<CommerceItem> items = new ArrayList<CommerceItem>();
        items.add(item1);
        JSONObject dataFields = new JSONObject();
        dataFields.put("field", "testValue");

        IterableApi.sharedInstance.trackPurchase(100.0, items, dataFields);

        RecordedRequest request = server.takeRequest();
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, request.getPath());
        assertEquals(expectedRequest, request.getBody().readUtf8());
    }

    @Test
    public void testUpdateEmailRequest() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        // Plain request check
        IterableApi.sharedInstance.updateEmail("test@example.com");

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/" + IterableConstants.ENDPOINT_UPDATE_EMAIL, request1.getPath());
        assertEquals("{\"currentEmail\":\"test_email\",\"newEmail\":\"test@example.com\"}", request1.getBody().readUtf8());
        Thread.sleep(100); // We need the callback to run to verify the internal email field change

        server.enqueue(new MockResponse().setResponseCode(400).setBody("{}"));

        // Check that we handle failures properly
        IterableApi.sharedInstance.updateEmail("invalid_mail!!123");

        RecordedRequest request2 = server.takeRequest();
        assertEquals("{\"currentEmail\":\"test@example.com\",\"newEmail\":\"invalid_mail!!123\"}", request2.getBody().readUtf8());
        Thread.sleep(100); // We need the callback to run to verify the internal email field change

        // Check that we still pass a valid (old) email after trying to update to an invalid one
        IterableApi.sharedInstance.updateEmail("another@email.com");

        RecordedRequest request3 = server.takeRequest();
        assertEquals("{\"currentEmail\":\"test@example.com\",\"newEmail\":\"another@email.com\"}", request3.getBody().readUtf8());
    }
}
