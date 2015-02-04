package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.core.RetryPolicyConfig;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.RetryPolicyConfigContainer;

import java.util.Arrays;

import static ru.taskurotta.service.hz.serialization.bson.SerializerTools.readArrayOfString;
import static ru.taskurotta.service.hz.serialization.bson.SerializerTools.writeArrayOfString;

/**
 * Created by greg on 03/02/15.
 */
public class RetryPolicyConfigContainerSerializer implements StreamBSerializer<RetryPolicyConfigContainer> {

    public static final CString EXCEPTIONS_TO_EXCLUDE = new CString("exceptionsToExclude");
    public static final CString EXCEPTIONS_TO_RETRY = new CString("exceptionToRetry");
    public static final CString INITIAL_RETRY_INTERVAL_SECONDS = new CString("initialRetryIntervalSeconds");
    public static final CString MAXIMUM_ATTEMPTS = new CString("maximumAttempts");
    public static final CString MAXIMUM_RETRY_INTERVAL_SECONDS = new CString("maximumRetryIntervalSecons");
    public static final CString RETRY_EXPIRATION_INTERVAL_SECONDS = new CString("retryExpirationIntervalSeconds");
    public static final CString TYPE = new CString("type");

    @Override
    public Class<RetryPolicyConfigContainer> getObjectClass() {
        return RetryPolicyConfigContainer.class;
    }

    @Override
    public void write(BDataOutput out, RetryPolicyConfigContainer object) {
        writeArrayOfString(EXCEPTIONS_TO_EXCLUDE, (String[]) object.getExceptionsToExclude().toArray(), out);
        writeArrayOfString(EXCEPTIONS_TO_RETRY, (String[]) object.getExceptionsToRetry().toArray(), out);
        out.writeLong(INITIAL_RETRY_INTERVAL_SECONDS, object.getInitialRetryIntervalSeconds());
        out.writeInt(MAXIMUM_ATTEMPTS, object.getMaximumAttempts());
        out.writeLong(MAXIMUM_RETRY_INTERVAL_SECONDS, object.getMaximumRetryIntervalSeconds());
        out.writeLong(RETRY_EXPIRATION_INTERVAL_SECONDS, object.getRetryExpirationIntervalSeconds());
        out.writeInt(TYPE, object.getType().getValue());
    }

    @Override
    public RetryPolicyConfigContainer read(BDataInput in) {
        String[] exceptionsToExclude = readArrayOfString(EXCEPTIONS_TO_EXCLUDE, in);
        String[] exceptionsToRetry = readArrayOfString(EXCEPTIONS_TO_RETRY, in);
        long initialRetryIntervalSeconds = in.readLong(INITIAL_RETRY_INTERVAL_SECONDS);
        int maximumAttempts = in.readInt(MAXIMUM_ATTEMPTS);
        long maximumRetryIntervalSeconds = in.readLong(MAXIMUM_RETRY_INTERVAL_SECONDS);
        long retryExpirationIntervalSeconds = in.readLong(RETRY_EXPIRATION_INTERVAL_SECONDS);
        RetryPolicyConfig.RetryPolicyType policyType = RetryPolicyConfig.RetryPolicyType.build(in.readInt(TYPE));

        RetryPolicyConfigContainer retryPolicyConfigContainer = new RetryPolicyConfigContainer();
        retryPolicyConfigContainer.setExceptionsToExclude((exceptionsToExclude != null) ? Arrays.asList(exceptionsToExclude) : null);
        retryPolicyConfigContainer.setExceptionsToRetry((exceptionsToRetry != null) ? Arrays.asList(exceptionsToRetry) : null);
        retryPolicyConfigContainer.setInitialRetryIntervalSeconds(initialRetryIntervalSeconds);
        retryPolicyConfigContainer.setMaximumAttempts(maximumAttempts);
        retryPolicyConfigContainer.setMaximumRetryIntervalSeconds(maximumRetryIntervalSeconds);
        retryPolicyConfigContainer.setRetryExpirationIntervalSeconds(retryExpirationIntervalSeconds);
        retryPolicyConfigContainer.setType(policyType);

        return retryPolicyConfigContainer;
    }
}
