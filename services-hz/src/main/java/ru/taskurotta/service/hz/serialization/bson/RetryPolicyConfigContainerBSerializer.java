package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.core.RetryPolicyConfig;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.RetryPolicyConfigContainer;

import java.util.List;

import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readListOfString;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeListOfString;

/**
 * Created by greg on 03/02/15.
 */
public class RetryPolicyConfigContainerBSerializer implements StreamBSerializer<RetryPolicyConfigContainer> {

    public static final CString TYPE = new CString("type");
    public static final CString INITIAL_RETRY_INTERVAL_SECONDS = new CString("inReIntSec");
    public static final CString MAXIMUM_RETRY_INTERVAL_SECONDS = new CString("maxIntSec");
    public static final CString RETRY_EXPIRATION_INTERVAL_SECONDS = new CString("reExIntSe");
    public static final CString BACKOFF_COEFFICIENT = new CString("backCoef");
    public static final CString MAXIMUM_ATTEMPTS = new CString("maxAtt");
    public static final CString EXCEPTIONS_TO_EXCLUDE = new CString("exToEx");
    public static final CString EXCEPTIONS_TO_RETRY = new CString("exToRe");

    @Override
    public Class<RetryPolicyConfigContainer> getObjectClass() {
        return RetryPolicyConfigContainer.class;
    }

    @Override
    public void write(BDataOutput out, RetryPolicyConfigContainer object) {
        out.writeInt(TYPE, object.getType().getValue());
        out.writeLong(INITIAL_RETRY_INTERVAL_SECONDS, object.getInitialRetryIntervalSeconds());
        out.writeLong(MAXIMUM_RETRY_INTERVAL_SECONDS, object.getMaximumRetryIntervalSeconds());
        out.writeLong(RETRY_EXPIRATION_INTERVAL_SECONDS, object.getRetryExpirationIntervalSeconds());
        out.writeDouble(BACKOFF_COEFFICIENT, object.getBackoffCoefficient());
        out.writeInt(MAXIMUM_ATTEMPTS, object.getMaximumAttempts());
        if (object.getExceptionsToExclude() != null) {
            writeListOfString(EXCEPTIONS_TO_EXCLUDE, object.getExceptionsToExclude(), out);
        }
        if (object.getExceptionsToRetry() != null) {
            writeListOfString(EXCEPTIONS_TO_RETRY, object.getExceptionsToRetry(), out);
        }
    }

    @Override
    public RetryPolicyConfigContainer read(BDataInput in) {
        RetryPolicyConfig.RetryPolicyType policyType = RetryPolicyConfig.RetryPolicyType.build(in.readInt(TYPE));
        long initialRetryIntervalSeconds = in.readLong(INITIAL_RETRY_INTERVAL_SECONDS);
        long maximumRetryIntervalSeconds = in.readLong(MAXIMUM_RETRY_INTERVAL_SECONDS);
        long retryExpirationIntervalSeconds = in.readLong(RETRY_EXPIRATION_INTERVAL_SECONDS);
        double backoffCoefficient = in.readDouble(BACKOFF_COEFFICIENT);
        int maximumAttempts = in.readInt(MAXIMUM_ATTEMPTS);

        List<String> exceptionsToExclude = readListOfString(EXCEPTIONS_TO_EXCLUDE, in);
        List<String> exceptionsToRetry = readListOfString(EXCEPTIONS_TO_RETRY, in);

        RetryPolicyConfigContainer retryPolicyConfigContainer = new RetryPolicyConfigContainer();
        retryPolicyConfigContainer.setInitialRetryIntervalSeconds(initialRetryIntervalSeconds);
        retryPolicyConfigContainer.setExceptionsToExclude(exceptionsToExclude);
        retryPolicyConfigContainer.setExceptionsToRetry(exceptionsToRetry);
        retryPolicyConfigContainer.setBackoffCoefficient(backoffCoefficient);
        retryPolicyConfigContainer.setMaximumAttempts(maximumAttempts);
        retryPolicyConfigContainer.setMaximumRetryIntervalSeconds(maximumRetryIntervalSeconds);
        retryPolicyConfigContainer.setRetryExpirationIntervalSeconds(retryExpirationIntervalSeconds);
        retryPolicyConfigContainer.setType(policyType);

        return retryPolicyConfigContainer;
    }
}
