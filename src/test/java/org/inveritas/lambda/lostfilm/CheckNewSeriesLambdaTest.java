package org.inveritas.lambda.lostfilm;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import org.testng.annotations.Test;

import java.util.Collections;

public class CheckNewSeriesLambdaTest {

    @Test
    public void testHandle() {
        CheckNewSeriesLambda lambda = new CheckNewSeriesLambda();
        SNSEvent event = new SNSEvent();
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage("{\"type\" : \"chime\",\n" +
                "  \"timestamp\": \"2015-05-26 02:15 UTC\",\n" +
                "  \"year\": \"2015\",\n" +
                "  \"month\": \"05\",\n" +
                "  \"day\": \"26\",\n" +
                "  \"hour\": \"02\",\n" +
                "  \"minute\": \"00\",\n" +
                "  \"day_of_week\": \"Tue\",\n" +
                "  \"unique_id\": \"2d135bf9-31ba-4751-b46d-1db6a822ac88\",\n" +
                "  \"region\": \"us-east-1\",\n" +
                "  \"sns_topic_arn\": \"arn:aws:sns:...\",\n" +
                "  \"reference\": \"...\",\n" +
                "  \"support\": \"...\",\n" +
                "  \"disclaimer\": \"UNRELIABLE SERVICE {ACCURACY,CONSISTENCY,UPTIME,LONGEVITY}\"}");
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setSns(sns);
        event.setRecords(Collections.singletonList(record));
        lambda.handleRequest(event, null);
    }
}
