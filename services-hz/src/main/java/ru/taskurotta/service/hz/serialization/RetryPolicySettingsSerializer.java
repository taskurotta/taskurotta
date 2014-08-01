package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.policy.retry.RetryPolicyConfig;

import java.io.IOException;

import static ru.taskurotta.service.hz.serialization.SerializationTools.readString;
import static ru.taskurotta.service.hz.serialization.SerializationTools.writeString;


/**
 * Created by greg
 */
public class RetryPolicySettingsSerializer implements StreamSerializer<RetryPolicyConfig> {

    @Override
    public void write(ObjectDataOutput out, RetryPolicyConfig retryPolicyConfig) throws IOException {
        out.writeInt(retryPolicyConfig.getType().getValue());
        out.writeLong(retryPolicyConfig.getInitialRetryIntervalSeconds());
        out.writeLong(retryPolicyConfig.getMaximumRetryIntervalSeconds());
        out.writeLong(retryPolicyConfig.getRetryExpirationIntervalSeconds());
        out.writeDouble(retryPolicyConfig.getBackoffCoefficient());
        out.writeInt(retryPolicyConfig.getMaximumAttempts());
        int exceptionsToRetrySize = retryPolicyConfig.getExceptionsToRetry().size();
        out.writeInt(exceptionsToRetrySize);
        if (exceptionsToRetrySize > 0) {
            for (String exceptionClass : retryPolicyConfig.getExceptionsToRetry()) {
                writeString(out, exceptionClass);
            }
        }
        int exceptionsToExcludeSize = retryPolicyConfig.getExceptionsToExclude().size();
        out.writeInt(exceptionsToExcludeSize);
        if (exceptionsToExcludeSize > 0) {
            for (String exceptionClass : retryPolicyConfig.getExceptionsToExclude()) {
                writeString(out, exceptionClass);
            }
        }
    }

    @Override
    public RetryPolicyConfig read(ObjectDataInput input) throws IOException {
        RetryPolicyConfig retryPolicyConfig = new RetryPolicyConfig();
        retryPolicyConfig.setType(RetryPolicyConfig.RetryPolicyType.build(input.readInt()));
        retryPolicyConfig.setInitialRetryIntervalSeconds(input.readLong());
        retryPolicyConfig.setMaximumRetryIntervalSeconds(input.readLong());
        retryPolicyConfig.setRetryExpirationIntervalSeconds(input.readLong());
        retryPolicyConfig.setBackoffCoefficient(input.readDouble());
        retryPolicyConfig.setMaximumAttempts(input.readInt());
        int exceptionsToRetrySize = input.readInt();
        if (exceptionsToRetrySize > 0) {
            for (int i = 0; i < exceptionsToRetrySize; i++) {
                retryPolicyConfig.getExceptionsToRetry().add(readString(input));
            }
        }
        int exceptionsToExcludeSize = input.readInt();
        if (exceptionsToExcludeSize > 0) {
            for (int i = 0; i < exceptionsToExcludeSize; i++) {
                retryPolicyConfig.getExceptionsToExclude().add(readString(input));
            }
        }
        return retryPolicyConfig;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.RETRY_POLICY_SETTINGS;
    }

    @Override
    public void destroy() {

    }
}
