package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.policy.retry.RetryPolicySettings;

import java.io.IOException;


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
