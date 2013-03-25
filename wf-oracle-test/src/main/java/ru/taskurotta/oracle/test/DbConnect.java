package ru.taskurotta.oracle.test;

import org.apache.commons.dbcp.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * User: greg
 */
public class DbConnect {

    private BasicDataSource dataSource;


    public DbConnect() {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        dataSource.setUsername("taskurotta");
        dataSource.setPassword("taskurotta");
        dataSource.setUrl("jdbc:oracle:thin:@//taskurotta-db:1521/orcl");
    }

    public BasicDataSource getDataSource() {
        return dataSource;
    }




}
