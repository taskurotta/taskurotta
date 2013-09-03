package ru.taskurotta.backend.ora;

import org.apache.commons.dbcp.BasicDataSource;

/**
 * User: greg
 * Used only for test enviroment
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
