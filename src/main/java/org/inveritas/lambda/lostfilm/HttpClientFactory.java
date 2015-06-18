package org.inveritas.lambda.lostfilm;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class HttpClientFactory implements AutoCloseable {
    public final static HttpClientFactory INSTANCE = new HttpClientFactory();

    private final HttpClient client;

    private HttpClientFactory() {
        client = create();
    }

    public static HttpClient getDefaultClient() {
        return INSTANCE.getClient();
    }

    private static HttpClient create() {
        SslContextFactory sslContextFactory = new SslContextFactory(true);
        HttpClient httpClient = new HttpClient(sslContextFactory);
        httpClient.setAddressResolutionTimeout(15000);
        httpClient.setConnectTimeout(15000);
        httpClient.setIdleTimeout(0);
        httpClient.setMaxConnectionsPerDestination(64);
        httpClient.setMaxRequestsQueuedPerDestination(1024);
        try {
            httpClient.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return httpClient;
    }

    @Override
    public void close() throws Exception {
        client.stop();
    }

    public HttpClient getClient() {
        return client;
    }
}
