package org.inveritas.lambda.lostfilm;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;

public class CheckNewSeriesLambda implements RequestHandler<SNSEvent, String> {

    public String handleRequest(SNSEvent snsEvent, Context context) {
        for (SNSEvent.SNSRecord record : snsEvent.getRecords()) {
            context.getLogger().log(record.getSNS().getMessage());
        }
        return "ok";
    }
}
