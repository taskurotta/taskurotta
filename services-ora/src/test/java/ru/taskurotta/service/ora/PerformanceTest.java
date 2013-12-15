package ru.taskurotta.service.ora;

import org.junit.Before;
import org.junit.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 01.10.13
 * Time: 15:46
 */
public class PerformanceTest {

    private DbConnect connection = new DbConnect();

    private Exception[] exceptions;

    private String[] processIds;
    private String[] actorIds;
    private String[] messages;
    private String[] stackTraces;

    @Before
    public void setUp() {
        int processesCount = 3;
        int actorsCount = 3;

        processIds = new String[processesCount];
        for (int i = 0; i < processesCount; i++) {
            processIds[i] = UUID.randomUUID().toString();
        }

        actorIds = new String[actorsCount];
        for (int i = 0; i < actorsCount; i++) {
            actorIds[i] = "test.actor.id#" + i + ".0";
        }

        exceptions = new Exception[]{new NullPointerException("NullPointerException"), new IllegalArgumentException("IllegalArgumentException"), new ArrayIndexOutOfBoundsException("ArrayIndexOutOfBoundsException")};
        int exceptionsCount = exceptions.length;
        messages = new String[exceptionsCount];
        stackTraces = new String[exceptionsCount];

        for (int i = 0; i < exceptionsCount; i++) {
            messages[i] = exceptions[i].getMessage();
            stackTraces[i] = stackTraceToString(exceptions[i].getStackTrace());
        }

        try (PreparedStatement preparedStatement = connection.getDataSource().getConnection().prepareStatement("truncate table errors")) {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String sql = "insert into errors (id, process_id, actor_id, message, stacktrace) values(errors_seq.nextval, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.getDataSource().getConnection().prepareStatement(sql)) {

            Random random = new Random();

            for (int i = 0; i < 1000; i++) {
                preparedStatement.setString(1, processIds[random.nextInt(processesCount)]);
                preparedStatement.setString(2, actorIds[random.nextInt(actorsCount)]);
                preparedStatement.setString(3, messages[random.nextInt(exceptionsCount)]);
                preparedStatement.setString(4, stackTraces[random.nextInt(exceptionsCount)]);

                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() {
        String selectProcessesSQL = "select process_id, count(process_id) from errors group by process_id";
        String selectActorsSQL = "select actor_id, count(actor_id) from errors group by actor_id";
        String selectMessagesSQL = "select message, count(message) from errors group by message";

        testSqlCountDuration(selectProcessesSQL, "Select different processes and count");

        testSqlCountDuration(selectActorsSQL, "Select different actors and count");

        testSqlCountDuration(selectMessagesSQL, "Select different messages and count");

        testSqlSearchDuration("select count(process_id) from errors where message = 'NullPointerException'", "Select processes with error = 'NullPointerException'");

        testSqlSearchDuration("select count(process_id) from errors where actor_id = 'test.actor.id#0.0'", "Select processes with actorId = 'test.actor.id#0.0'");
    }

    private void testSqlCountDuration(String sql, String message) {

        System.out.println("--------");

        try (CallableStatement callableStatement = connection.getDataSource().getConnection().prepareCall(sql)) {
            long start = System.currentTimeMillis();
            callableStatement.execute();
            long duration = System.currentTimeMillis() - start;

            ResultSet resultSet = callableStatement.executeQuery();
            while (resultSet.next()) {
                System.out.println(resultSet.getString(1) + " = " + resultSet.getString(2));
            }

            System.out.println(message + " for [" + duration + "] mls");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void testSqlSearchDuration(String sql, String message) {
        System.out.println("--------");

        try (CallableStatement callableStatement = connection.getDataSource().getConnection().prepareCall(sql)) {
            long start = System.currentTimeMillis();
            callableStatement.execute();
            long duration = System.currentTimeMillis() - start;

            ResultSet resultSet = callableStatement.executeQuery();
            while (resultSet.next()) {
                System.out.println(message + " = " + resultSet.getString(1));
            }

            System.out.println(message + " for [" + duration + "] mls");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String stackTraceToString(StackTraceElement[] stackTraceElements) {
        StringBuilder stringBuilder = new StringBuilder(stackTraceElements.length);

        for (StackTraceElement stackTraceElement : stackTraceElements) {
            stringBuilder.append(stackTraceElement.toString());
        }

        return stringBuilder.toString();
    }
}
