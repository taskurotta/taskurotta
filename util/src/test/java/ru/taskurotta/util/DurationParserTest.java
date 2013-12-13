package ru.taskurotta.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 13.12.13
 * Time: 17:48
 */
public class DurationParserTest {
    @Test
    public void testToMillis() throws Exception {
        assertEquals(10000, DurationParser.toMillis("10000 milliseconds"));
        assertEquals(10000, DurationParser.toMillis("10 seconds"));
        assertEquals(10000, DurationParser.toMillis("10seConds"));
    }
}
