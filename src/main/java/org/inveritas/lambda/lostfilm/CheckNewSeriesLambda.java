package org.inveritas.lambda.lostfilm;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class CheckNewSeriesLambda implements AWSLambda {
    public String handler(int myCount, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("received : " + myCount);
        return String.valueOf(myCount);
    }
}
