package ru.taskurotta.policy;

/**
 * User: stukushin
 * Date: 21.02.13
 * Time: 13:05
 */
public class PolicyConstants {
    public static final int NONE = -1;
    public static final long EXPONENTIAL_RETRY_MAXIMUM_RETRY_INTERVAL_SECONDS =  NONE;
    public static final long EXPONENTIAL_RETRY_RETRY_EXPIRATION_SECONDS =  NONE;
    public static final double EXPONENTIAL_RETRY_BACKOFF_COEFFICIENT =  2;
    public static final int EXPONENTIAL_RETRY_MAXIMUM_ATTEMPTS =  NONE;
}
