package com.amazonaws.http.timers.request;

import static com.amazonaws.http.timers.ClientExecutionAndRequestTimerTestUtils.assertNumberOfRetries;
import static com.amazonaws.http.timers.ClientExecutionAndRequestTimerTestUtils.*;
import static com.amazonaws.http.timers.ClientExecutionAndRequestTimerTestUtils.execute;
import static com.amazonaws.http.timers.TimeoutTestConstants.TEST_TIMEOUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.TestPreConditions;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.http.HttpClientFactory;
import com.amazonaws.http.MockServerTestBase;
import com.amazonaws.http.server.MockServer;

/**
 * Tests that use a server that returns a predetermined response within the timeout limit
 */
public class DummyResponseServerTests extends MockServerTestBase {

    private static final int STATUS_CODE = 500;
    private AmazonHttpClient httpClient;

    @BeforeClass
    public static void preConditions() {
        TestPreConditions.assumeNotJava6();
    }

    @Override
    protected MockServer buildMockServer() {
        return new MockServer(
                MockServer.DummyResponseServerBehavior.build(STATUS_CODE, "Internal Server Failure", "Dummy response"));
    }

    @Test(timeout = TEST_TIMEOUT)
    public void requestTimeoutEnabled_ServerRespondsWithRetryableError_RetriesUpToLimitThenThrowsServerException()
            throws IOException {
        int maxRetries = 2;
        ClientConfiguration config = new ClientConfiguration().withRequestTimeout(25 * 1000)
                .withClientExecutionTimeout(25 * 1000).withMaxErrorRetry(maxRetries);
        HttpClientFactory httpClientFactory = new HttpClientFactory();
        HttpClient rawHttpClient = spy(httpClientFactory.createHttpClient(config));

        httpClient = new AmazonHttpClient(config, rawHttpClient, null);

        try {
            execute(httpClient, newGetRequest());
            fail("Exception expected");
        } catch (AmazonServiceException e) {
            assertEquals(e.getStatusCode(), STATUS_CODE);
            int expectedNumberOfRequests = 1 + maxRetries;
            assertNumberOfRetries(rawHttpClient, expectedNumberOfRequests);
            assertNumberOfTasksTriggered(httpClient.getHttpRequestTimer(), 0);
            assertNumberOfTasksTriggered(httpClient.getClientExecutionTimer(), 0);
        }
    }

}