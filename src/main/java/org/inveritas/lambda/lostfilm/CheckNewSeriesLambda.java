package org.inveritas.lambda.lostfilm;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class CheckNewSeriesLambda implements RequestHandler<SNSEvent, String> {
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public String handleRequest(SNSEvent snsEvent, Context context) {
        for (SNSEvent.SNSRecord record : snsEvent.getRecords()) {
            try {
                ChimeMessage msg = mapper.readValue(record.getSNS().getMessage(), ChimeMessage.class);
                if (!msg.getMinute().equals("00")) {
                    return "ok";
                }

                HttpClient httpClient = HttpClientFactory.getDefaultClient();
                InputStreamResponseListener listener = new InputStreamResponseListener();
                httpClient.newRequest("http://www.lostfilm.tv/rssdd.xml").send(listener);
                Response response = listener.get(5, TimeUnit.SECONDS);
                if (response.getStatus() == 200) {
                    XMLStreamReader xmlp = null;
                    try (InputStream is = listener.getInputStream()) {
                        XMLInputFactory xmlFactory = XMLInputFactory.newFactory();
                        xmlp = xmlFactory.createXMLStreamReader(is, "windows-1251");
                        while (xmlp.hasNext()) {
                            xmlp.next();
                            if (xmlp.isStartElement() && xmlp.getName().toString().equals("lastBuildDate")) {
                                String lastDate = xmlp.getElementText();
                                ZonedDateTime dateTime = ZonedDateTime.parse(lastDate, DateTimeFormatter.RFC_1123_DATE_TIME);

                                URL fileNameResource = getClass().getClassLoader().getResource("aws_creds");
                                Properties props = new Properties();
                                try (InputStream awsPropsIs = (InputStream) fileNameResource.getContent()) {
                                    props.load(awsPropsIs);
                                }
                                BasicAWSCredentials awsCreds = new BasicAWSCredentials(props.getProperty("aws.accessKeyId"), props.getProperty("aws.secretKey"));
                                AmazonS3 s3Client = new AmazonS3Client(awsCreds);
                                S3Object s3Object = null;
                                try {
                                    s3Object = s3Client.getObject(new GetObjectRequest("lostfilm", "lastBuildDate"));
                                } catch (AmazonS3Exception e) {
                                    if (e.getStatusCode() == 404) {
                                        s3Client.putObject(new PutObjectRequest("lostfilm", "lastBuildDate", new ByteArrayInputStream(lastDate.getBytes(StandardCharsets.UTF_8)), null));
                                        return "ok";
                                    }
                                }
                                try (S3ObjectInputStream s3is = s3Object.getObjectContent();
                                     Scanner s = new Scanner(s3is)) {
                                    String savedDateTimeStr = s.useDelimiter("\\A").hasNext() ? s.next() : "";
//                                    savedDateTimeStr = "Thu, 18 Jun 2015 17:01:05 +0000";
                                    ZonedDateTime savedDateTime = ZonedDateTime.parse(savedDateTimeStr, DateTimeFormatter.RFC_1123_DATE_TIME);
                                    if (dateTime.isAfter(savedDateTime)) {
                                        List<Series> newSeries = new ArrayList<>();
                                        Series series = null;
                                        while (xmlp.hasNext()) {
                                            xmlp.next();
                                            if (xmlp.isStartElement() && xmlp.getName().toString().equals("item")) {
                                                series = new Series();
                                            } else if (xmlp.isStartElement() && xmlp.getName().toString().equals("title")) {
                                                series.setTitle(xmlp.getElementText());
                                            } else if (xmlp.isStartElement() && xmlp.getName().toString().equals("pubDate")) {
                                                String pubDate = xmlp.getElementText();
                                                ZonedDateTime pubDateTime = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
                                                if (pubDateTime.isAfter(savedDateTime)) {
                                                    newSeries.add(series);
                                                } else {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    } finally {
                        if (xmlp != null) {
                            xmlp.close();
                        }
                    }
                }
            } catch (Exception e) {
                context.getLogger().log(e.getMessage());
            }
        }
        return "ok";
    }
}
