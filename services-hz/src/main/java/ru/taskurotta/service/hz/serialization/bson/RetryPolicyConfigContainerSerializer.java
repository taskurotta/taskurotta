package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.core.RetryPolicyConfig;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.RetryPolicyConfigContainer;

import java.util.List;

import static ru.taskurotta.service.hz.serialization.bson.SerializerTools.readListOfString;
import static ru.taskurotta.service.hz.serialization.bson.SerializerTools.writeListOfString;

/**
 * Created by greg on 03/02/15.
 */
public class RetryPolicyConfigContainerSerializer implements StreamBSerializer<RetryPolicyConfigContainer> {

    public static final CString EXCEPTIONS_TO_EXCLUDE = new CString("exToEx");
    public static final CString EXCEPTIONS_TO_RETRY = new CString("exToRe");
    public static final CString INITIAL_RETRY_INTERVAL_SECONDS = new CString("inReIntSec");
    public static final CString MAXIMUM_ATTEMPTS = new CString("maxAtt");
    public static final CString MAXIMUM_RETRY_INTERVAL_SECONDS = new CString("maxIntSec");
    public static final CString RETRY_EXPIRATION_INTERVAL_SECONDS = new CString("reExIntSe");
    public static final CString TYPE = new CString("type");
    public static final CString BACKOFF_COEFFICIENT = new CString("backCoef");

    @Override
    public Class<RetryPolicyConfigContainer> getObjectClass() {
        return RetryPolicyConfigContainer.class;
    }

    @Override
    public void write(BDataOutput out, RetryPolicyConfigContainer object) {
        if (object.getExceptionsToExclude() != null) {
            writeListOfString(EXCEPTIONS_TO_EXCLUDE, object.getExceptionsToExclude(), out);
        }
        if (object.getExceptionsToRetry() != null) {
            writeListOfString(EXCEPTIONS_TO_RETRY, object.getExceptionsToRetry(), out);
        }
        out.writeDouble(BACKOFF_COEFFICIENT, object.getBackoffCoefficient());
        out.writeLong(INITIAL_RETRY_INTERVAL_SECONDS, object.getInitialRetryIntervalSeconds());
        out.writeInt(MAXIMUM_ATTEMPTS, object.getMaximumAttempts());
        out.writeLong(MAXIMUM_RETRY_INTERVAL_SECONDS, object.getMaximumRetryIntervalSeconds());
        out.writeLong(RETRY_EXPIRATION_INTERVAL_SECONDS, object.getRetryExpirationIntervalSeconds());
        out.writeInt(TYPE, object.getType().getValue());
    }

    @Override
    public RetryPolicyConfigContainer read(BDataInput in) {
        List<String> exceptionsToExclude = readListOfString(EXCEPTIONS_TO_EXCLUDE, in);
        List<String> exceptionsToRetry = readListOfString(EXCEPTIONS_TO_RETRY, in);
        double backoff = in.readDouble(BACKOFF_COEFFICIENT);
        long initialRetryIntervalSeconds = in.readLong(INITIAL_RETRY_INTERVAL_SECONDS);
        int maximumAttempts = in.readInt(MAXIMUM_ATTEMPTS);
        long maximumRetryIntervalSeconds = in.readLong(MAXIMUM_RETRY_INTERVAL_SECONDS);
        long retryExpirationIntervalSeconds = in.readLong(RETRY_EXPIRATION_INTERVAL_SECONDS);
        RetryPolicyConfig.RetryPolicyType policyType = RetryPolicyConfig.RetryPolicyType.build(in.readInt(TYPE));

        RetryPolicyConfigContainer retryPolicyConfigContainer = new RetryPolicyConfigContainer();
        retryPolicyConfigContainer.setExceptionsToExclude(exceptionsToExclude);
        retryPolicyConfigContainer.setExceptionsToRetry(exceptionsToRetry);
        retryPolicyConfigContainer.setBackoffCoefficient(backoff);
        retryPolicyConfigContainer.setInitialRetryIntervalSeconds(initialRetryIntervalSeconds);
        retryPolicyConfigContainer.setMaximumAttempts(maximumAttempts);
        retryPolicyConfigContainer.setMaximumRetryIntervalSeconds(maximumRetryIntervalSeconds);
        retryPolicyConfigContainer.setRetryExpirationIntervalSeconds(retryExpirationIntervalSeconds);
        retryPolicyConfigContainer.setType(policyType);

        return retryPolicyConfigContainer;
    }
}
