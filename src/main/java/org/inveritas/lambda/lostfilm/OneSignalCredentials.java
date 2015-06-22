package org.inveritas.lambda.lostfilm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class OneSignalCredentials {
    private final Properties props;

    private OneSignalCredentials() {
        URL fileNameResource = getClass().getClassLoader().getResource("onesignal_creds");
        Properties props = new Properties();
        try (InputStream awsPropsIs = (InputStream) fileNameResource.getContent()) {
            props.load(awsPropsIs);
            this.props = props;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class SingletonHolder {
        public static final OneSignalCredentials HOLDER_INSTANCE = new OneSignalCredentials();
    }

    public static OneSignalCredentials getInstance() {
        return SingletonHolder.HOLDER_INSTANCE;
    }

    public String getAppId() {
        return props.getProperty("app_id");
    }

    public String getRestKey() {
        return props.getProperty("rest_key");
    }
}
