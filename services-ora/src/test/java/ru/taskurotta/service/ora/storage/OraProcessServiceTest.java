package ru.taskurotta.service.ora.storage;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.taskurotta.service.common.ResultSetCursor;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 15.05.2015
 * Time: 13:03
 */

@Ignore
public class OraProcessServiceTest {

    private OraProcessService oraProcessService;

    @Before
    public void setUp() throws Exception {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");

        this.oraProcessService = new OraProcessService(dataSource);
    }

    @Test
    public void testFinishProcess() throws Exception {

    }

    @Test
    public void testDeleteProcess() throws Exception {

    }

    @Test
    public void testStartProcess() throws Exception {

    }

    @Test
    public void testGetProcess() throws Exception {

    }

    @Test
    public void testGetStartTask() throws Exception {

    }

    @Test
    public void testMarkProcessAsBroken() throws Exception {

    }

    @Test
    public void testMarkProcessAsStarted() throws Exception {

    }

    @Test
    public void testMarkProcessAsAborted() throws Exception {

    }

    @Test
    public void testListProcesses() throws Exception {

    }

    @Test
    public void testFindProcesses() throws Exception {

    }

    @Test
    public void testGetFinishedCount() throws Exception {

    }

    @Test
    public void testFindIncompleteProcesses() throws Exception {
        ResultSetCursor<UUID> resultSetCursor = oraProcessService.findIncompleteProcesses(System.currentTimeMillis(), 1);

        int count = 0;
        int size = resultSetCursor.getNext().size();
        while (size > 0) {
            count += size;
            size = resultSetCursor.getNext().size();
        }
        System.out.println(count);

        resultSetCursor.close();
    }

    @Test
    public void testGetBrokenProcessCount() throws Exception {

    }
}