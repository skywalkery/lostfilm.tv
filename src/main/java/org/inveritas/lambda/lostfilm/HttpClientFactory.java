package org.inveritas.lambda.lostfilm;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.entity.ContentType;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.inveritas.lambda.lostfilm.notification.msg.CreateNotificationMessage;
import org.inveritas.lambda.lostfilm.notification.msg.EnLocalizedMessage;
import org.inveritas.lambda.lostfilm.notification.msg.Tag;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class HttpClientFactory implements AutoCloseable {
    private static final String ONESIGNAL_NOTIFICATION = "https://onesignal.com/api/v1/notifications";

    public static final HttpClientFactory INSTANCE = new HttpClientFactory();

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

    public Request getNotificationRequest(String title, String message, String serial, String icoUrl) throws JsonProcessingException {
        CreateNotificationMessage msg = new CreateNotificationMessage();
        msg.setContents(new EnLocalizedMessage(message));
        msg.setHeadings(new EnLocalizedMessage(title));
        msg.setIsAndroid(true);
        msg.setLargeIconUrl(icoUrl);
        List<Tag> tags = Collections.singletonList(new Tag("serial", "=", serial));
        msg.setTags(tags);
        msg.setAppId(OneSignalCredentials.getInstance().getAppId());
        return client.POST(ONESIGNAL_NOTIFICATION)
                .header("authorization", "Basic " + OneSignalCredentials.getInstance().getRestKey())
                .content(new StringContentProvider(ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8).toString(),
                        CheckNewSeriesLambda.MAPPER.writeValueAsString(msg),
                        StandardCharsets.UTF_8));
    }
}
