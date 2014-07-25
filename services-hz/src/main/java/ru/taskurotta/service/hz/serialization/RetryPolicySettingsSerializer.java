package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.policy.retry.RetryPolicySettings;

import java.io.IOException;

import static ru.taskurotta.service.hz.serialization.SerializationTools.readString;
import static ru.taskurotta.service.hz.serialization.SerializationTools.writeString;


/**
 * Created by greg
 */
public class RetryPolicySettingsSerializer implements StreamSerializer<RetryPolicySettings> {

    @Override
    public void write(ObjectDataOutput out, RetryPolicySettings retryPolicySettings) throws IOException {
        out.writeInt(retryPolicySettings.getType().getValue());
        out.writeLong(retryPolicySettings.getInitialRetryIntervalSeconds());
        out.writeLong(retryPolicySettings.getMaximumRetryIntervalSeconds());
        out.writeLong(retryPolicySettings.getRetryExpirationIntervalSeconds());
        out.writeDouble(retryPolicySettings.getBackoffCoefficient());
        out.writeInt(retryPolicySettings.getMaximumAttempts());
        int exceptionsToRetrySize = retryPolicySettings.getExceptionsToRetry().size();
        out.writeInt(exceptionsToRetrySize);
        if (exceptionsToRetrySize > 0) {
            for (String exceptionClass : retryPolicySettings.getExceptionsToRetry()) {
                writeString(out, exceptionClass);
            }
        }
        int exceptionsToExcludeSize = retryPolicySettings.getExceptionsToExclude().size();
        out.writeInt(exceptionsToExcludeSize);
        if (exceptionsToExcludeSize > 0) {
            for (String exceptionClass : retryPolicySettings.getExceptionsToExclude()) {
                writeString(out, exceptionClass);
            }
        }
    }

    @Override
    public RetryPolicySettings read(ObjectDataInput input) throws IOException {
        RetryPolicySettings retryPolicySettings = new RetryPolicySettings();
        retryPolicySettings.setType(RetryPolicySettings.RetryPolicyType.build(input.readInt()));
        retryPolicySettings.setInitialRetryIntervalSeconds(input.readLong());
        retryPolicySettings.setMaximumRetryIntervalSeconds(input.readLong());
        retryPolicySettings.setRetryExpirationIntervalSeconds(input.readLong());
        retryPolicySettings.setBackoffCoefficient(input.readDouble());
        retryPolicySettings.setMaximumAttempts(input.readInt());
        int exceptionsToRetrySize = input.readInt();
        if (exceptionsToRetrySize > 0) {
            for (int i = 0; i < exceptionsToRetrySize; i++) {
                retryPolicySettings.getExceptionsToRetry().add(readString(input));
            }
        }
        int exceptionsToExcludeSize = input.readInt();
        if (exceptionsToExcludeSize > 0) {
            for (int i = 0; i < exceptionsToExcludeSize; i++) {
                retryPolicySettings.getExceptionsToExclude().add(readString(input));
            }
        }
        return retryPolicySettings;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.RETRY_POLICY_SETTINGS;
    }

    @Override
    public void destroy() {

    }
}
