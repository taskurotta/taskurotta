package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.core.RetryPolicyConfig;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.RetryPolicyConfigContainer;

import java.util.Collections;
import java.util.List;

import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readListOfString;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeListOfString;

public class RetryPolicyConfigContainerBSerializer implements StreamBSerializer<RetryPolicyConfigContainer> {

    public static final CString TYPE = new CString("type");
    public static final CString INITIAL_RETRY = new CString("start");
    public static final CString MAXIMUM_RETRY = new CString("max");
    public static final CString RETRY_EXPIRATION = new CString("retryExp");
    public static final CString BACKOFF_COEFFICIENT = new CString("backoffCoefficient");
    public static final CString MAXIMUM_ATTEMPTS = new CString("maxAttempts");
    public static final CString EXCEPTIONS_TO_EXCLUDE = new CString("exExclude");
    public static final CString EXCEPTIONS_TO_RETRY = new CString("eRetry");

    @Override
    public Class<RetryPolicyConfigContainer> getObjectClass() {
        return RetryPolicyConfigContainer.class;
    }

    @Override
    public void write(BDataOutput out, RetryPolicyConfigContainer object) {
        out.writeInt(TYPE, object.getType().getValue());
        out.writeLong(INITIAL_RETRY, object.getInitialRetryIntervalSeconds());
        out.writeLong(MAXIMUM_RETRY, object.getMaximumRetryIntervalSeconds());
        out.writeLong(RETRY_EXPIRATION, object.getRetryExpirationIntervalSeconds());
        out.writeDouble(BACKOFF_COEFFICIENT, object.getBackoffCoefficient());
        out.writeInt(MAXIMUM_ATTEMPTS, object.getMaximumAttempts());
        writeListOfString(EXCEPTIONS_TO_EXCLUDE, object.getExceptionsToExclude(), out);
        writeListOfString(EXCEPTIONS_TO_RETRY, object.getExceptionsToRetry(), out);
    }

    @Override
    public RetryPolicyConfigContainer read(BDataInput in) {
        RetryPolicyConfig.RetryPolicyType policyType = RetryPolicyConfig.RetryPolicyType.build(in.readInt(TYPE));
        long initialRetryIntervalSeconds = in.readLong(INITIAL_RETRY);
        long maximumRetryIntervalSeconds = in.readLong(MAXIMUM_RETRY);
        long retryExpirationIntervalSeconds = in.readLong(RETRY_EXPIRATION);
        double backoffCoefficient = in.readDouble(BACKOFF_COEFFICIENT);
        int maximumAttempts = in.readInt(MAXIMUM_ATTEMPTS);

        List<String> exceptionsToExclude = readListOfString(EXCEPTIONS_TO_EXCLUDE, in, Collections.<String>emptyList());
        List<String> exceptionsToRetry = readListOfString(EXCEPTIONS_TO_RETRY, in, Collections.<String>emptyList());

        return new RetryPolicyConfigContainer(policyType, initialRetryIntervalSeconds, maximumRetryIntervalSeconds,
                retryExpirationIntervalSeconds, backoffCoefficient, maximumAttempts, exceptionsToRetry, exceptionsToExclude);
    }
}
