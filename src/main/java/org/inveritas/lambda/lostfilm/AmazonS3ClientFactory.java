package org.inveritas.lambda.lostfilm;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class AmazonS3ClientFactory {
    private final AmazonS3 client;

    private AmazonS3ClientFactory() {
        URL fileNameResource = getClass().getClassLoader().getResource("aws_creds");
        Properties props = new Properties();
        try (InputStream awsPropsIs = (InputStream) fileNameResource.getContent()) {
            props.load(awsPropsIs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(props.getProperty("aws.accessKeyId"), props.getProperty("aws.secretKey"));
        client = new com.amazonaws.services.s3.AmazonS3Client(awsCreds);
    }

    public static class SingletonHolder {
        public static final AmazonS3ClientFactory HOLDER_INSTANCE = new AmazonS3ClientFactory();
    }

    public static AmazonS3ClientFactory getInstance() {
        return SingletonHolder.HOLDER_INSTANCE;
    }

    public AmazonS3 getClient() {
        return client;
    }
}
