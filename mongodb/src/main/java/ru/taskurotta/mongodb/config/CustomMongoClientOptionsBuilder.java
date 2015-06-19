package ru.taskurotta.mongodb.config;

import com.mongodb.MongoClientOptions;
import com.mongodb.WriteConcern;

/**
 * User: stukushin
 * Date: 18.06.2015
 * Time: 14:12
 */

public class CustomMongoClientOptionsBuilder extends MongoClientOptions.Builder {

    public CustomMongoClientOptionsBuilder(int minConnectionsPerHost, int connectionsPerHost,
                                           int threadsAllowedToBlockForConnectionMultiplier, int maxWaitTime,
                                           int maxConnectionIdleTime, int maxConnectionLifeTime, int connectTimeout,
                                           int socketTimeout, boolean socketKeepAlive, WriteConcern writeConcern) {

        MongoClientOptions.Builder builder = MongoClientOptions.builder();
        builder.minConnectionsPerHost(minConnectionsPerHost).
                connectionsPerHost(connectionsPerHost).
                threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier).
                maxWaitTime(maxWaitTime).
                maxConnectionIdleTime(maxConnectionIdleTime).
                maxConnectionLifeTime(maxConnectionLifeTime).
                connectTimeout(connectTimeout).
                socketTimeout(socketTimeout).
                socketKeepAlive(socketKeepAlive).
                writeConcern(writeConcern);
    }
}
