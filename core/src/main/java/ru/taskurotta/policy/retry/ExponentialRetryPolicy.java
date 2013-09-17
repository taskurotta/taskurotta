/*
 * Copyright 2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License is
 * located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package ru.taskurotta.policy.retry;

import ru.taskurotta.policy.PolicyConstants;

import java.util.Collection;

public class ExponentialRetryPolicy extends TimeRetryPolicyBase {

    public ExponentialRetryPolicy(long initialRetryIntervalSeconds) {
        this.initialRetryIntervalSeconds = initialRetryIntervalSeconds;
    }

    public ExponentialRetryPolicy withMaximumRetryIntervalSeconds(long maximumRetryIntervalSeconds) {
        this.maximumRetryIntervalSeconds = maximumRetryIntervalSeconds;
        return this;
    }

    public ExponentialRetryPolicy withRetryExpirationIntervalSeconds(long retryExpirationIntervalSeconds) {
        this.retryExpirationIntervalSeconds = retryExpirationIntervalSeconds;
        return this;
    }

    public ExponentialRetryPolicy withBackoffCoefficient(double backoffCoefficient) {
        this.backoffCoefficient = backoffCoefficient;
        return this;
    }

    public ExponentialRetryPolicy withMaximumAttempts(int maximumAttempts) {
        this.maximumAttempts = maximumAttempts;
        return this;
    }

    /**
     * The exception types that cause operation being retried. Subclasses of the
     * specified types are also included. Default is Throwable.class which means
     * retry any exceptions.
     */
    @Override
    public ExponentialRetryPolicy withExceptionsToRetry(Collection<Class<? extends Throwable>> exceptionsToRetry) {
        super.withExceptionsToRetry(exceptionsToRetry);
        return this;
    }

    /**
     * The exception types that should not be retried. Subclasses of the
     * specified types are also not retried. Default is empty list.
     */
    @Override
    public ExponentialRetryPolicy withExceptionsToExclude(Collection<Class<? extends Throwable>> exceptionsToRetry) {
        super.withExceptionsToExclude(exceptionsToRetry);
        return this;
    }

    @Override
    public long nextRetryDelaySeconds(long firstAttempt, long recordedFailure, int numberOfTries) {

        if (maximumAttempts > PolicyConstants.NONE && numberOfTries > maximumAttempts) {
            return PolicyConstants.NONE;
        }

        long result = (long) (initialRetryIntervalSeconds * Math.pow(backoffCoefficient, numberOfTries));
        result = maximumRetryIntervalSeconds > PolicyConstants.NONE ? Math.min(result, maximumRetryIntervalSeconds) : result;
        int secondsSinceFirstAttempt = (int) ((recordedFailure - firstAttempt) / 1000);
        if (retryExpirationIntervalSeconds > PolicyConstants.NONE
                && secondsSinceFirstAttempt + result >= retryExpirationIntervalSeconds) {
            return PolicyConstants.NONE;
        }

        return result;
    }

    /**
     * Performs the following three validation checks for ExponentialRetry
     * Policy: 1) initialRetryIntervalSeconds is not greater than
     * maximumRetryIntervalSeconds 2) initialRetryIntervalSeconds is not greater
     * than retryExpirationIntervalSeconds
     */
    public void validate() throws IllegalStateException {
        if (maximumRetryIntervalSeconds > PolicyConstants.NONE && initialRetryIntervalSeconds > maximumRetryIntervalSeconds) {
            throw new IllegalStateException(
                    "ExponentialRetryPolicy requires maximumRetryIntervalSeconds to have a value larger than initialRetryIntervalSeconds.");
        }

        if (retryExpirationIntervalSeconds > PolicyConstants.NONE && initialRetryIntervalSeconds > retryExpirationIntervalSeconds) {
            throw new IllegalStateException(
                    "ExponentialRetryPolicy requires retryExpirationIntervalSeconds to have a value larger than initialRetryIntervalSeconds.");
        }
    }
}
