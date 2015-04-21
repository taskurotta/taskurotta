package ru.taskurotta.bootstrap;

import org.junit.Test;
import org.slf4j.Marker;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;

/**
 */
public class ActorExecutorTest {

    public static int printedError = 0;

    @Test
    public void errorOverflow() throws NoSuchFieldException, IllegalAccessException {
        ActorExecutor actorExecutor = getModeledActorExecutor();

        for (int i = 0; i < 10; i++) {
            actorExecutor.logError("Msg", new Exception());
        }

        assertEquals(printedError, 1);

        for (int i = 0; i < 10; i++) {
            actorExecutor.logError("Msg", new Exception(null, new Exception()));
        }

        assertEquals(printedError, 2);

        for (int i = 0; i < 10; i++) {
            actorExecutor.logError("Msg", new Exception("test", new Exception()));
        }

        assertEquals(printedError, 3);

        for (int i = 0; i < 10; i++) {
            actorExecutor.logError("Msg", new Exception("test"));
        }

        assertEquals(printedError, 4);
    }

    private ActorExecutor getModeledActorExecutor() throws NoSuchFieldException, IllegalAccessException {

        // create instance
        ActorExecutor actorExecutor = new ActorExecutor(new SimpleProfiler(), new Inspector(null, null), null, null);

        // set mock logger
        Field field = ActorExecutor.class.getDeclaredField("logger");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, new org.slf4j.Logger() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public boolean isTraceEnabled() {
                return false;
            }

            @Override
            public void trace(String msg) {

            }

            @Override
            public void trace(String format, Object arg) {

            }

            @Override
            public void trace(String format, Object arg1, Object arg2) {

            }

            @Override
            public void trace(String format, Object... arguments) {

            }

            @Override
            public void trace(String msg, Throwable t) {

            }

            @Override
            public boolean isTraceEnabled(Marker marker) {
                return false;
            }

            @Override
            public void trace(Marker marker, String msg) {

            }

            @Override
            public void trace(Marker marker, String format, Object arg) {

            }

            @Override
            public void trace(Marker marker, String format, Object arg1, Object arg2) {

            }

            @Override
            public void trace(Marker marker, String format, Object... argArray) {

            }

            @Override
            public void trace(Marker marker, String msg, Throwable t) {

            }

            @Override
            public boolean isDebugEnabled() {
                return false;
            }

            @Override
            public void debug(String msg) {

            }

            @Override
            public void debug(String format, Object arg) {

            }

            @Override
            public void debug(String format, Object arg1, Object arg2) {

            }

            @Override
            public void debug(String format, Object... arguments) {

            }

            @Override
            public void debug(String msg, Throwable t) {

            }

            @Override
            public boolean isDebugEnabled(Marker marker) {
                return false;
            }

            @Override
            public void debug(Marker marker, String msg) {

            }

            @Override
            public void debug(Marker marker, String format, Object arg) {

            }

            @Override
            public void debug(Marker marker, String format, Object arg1, Object arg2) {

            }

            @Override
            public void debug(Marker marker, String format, Object... arguments) {

            }

            @Override
            public void debug(Marker marker, String msg, Throwable t) {

            }

            @Override
            public boolean isInfoEnabled() {
                return false;
            }

            @Override
            public void info(String msg) {

            }

            @Override
            public void info(String format, Object arg) {

            }

            @Override
            public void info(String format, Object arg1, Object arg2) {

            }

            @Override
            public void info(String format, Object... arguments) {

            }

            @Override
            public void info(String msg, Throwable t) {

            }

            @Override
            public boolean isInfoEnabled(Marker marker) {
                return false;
            }

            @Override
            public void info(Marker marker, String msg) {

            }

            @Override
            public void info(Marker marker, String format, Object arg) {

            }

            @Override
            public void info(Marker marker, String format, Object arg1, Object arg2) {

            }

            @Override
            public void info(Marker marker, String format, Object... arguments) {

            }

            @Override
            public void info(Marker marker, String msg, Throwable t) {

            }

            @Override
            public boolean isWarnEnabled() {
                return false;
            }

            @Override
            public void warn(String msg) {

            }

            @Override
            public void warn(String format, Object arg) {

            }

            @Override
            public void warn(String format, Object... arguments) {

            }

            @Override
            public void warn(String format, Object arg1, Object arg2) {

            }

            @Override
            public void warn(String msg, Throwable t) {

            }

            @Override
            public boolean isWarnEnabled(Marker marker) {
                return false;
            }

            @Override
            public void warn(Marker marker, String msg) {

            }

            @Override
            public void warn(Marker marker, String format, Object arg) {

            }

            @Override
            public void warn(Marker marker, String format, Object arg1, Object arg2) {

            }

            @Override
            public void warn(Marker marker, String format, Object... arguments) {

            }

            @Override
            public void warn(Marker marker, String msg, Throwable t) {

            }

            @Override
            public boolean isErrorEnabled() {
                return false;
            }

            @Override
            public void error(String msg) {

            }

            @Override
            public void error(String format, Object arg) {

            }

            @Override
            public void error(String format, Object arg1, Object arg2) {

            }

            @Override
            public void error(String format, Object... arguments) {

            }

            @Override
            public void error(String msg, Throwable t) {
                printedError ++;
            }

            @Override
            public boolean isErrorEnabled(Marker marker) {
                return false;
            }

            @Override
            public void error(Marker marker, String msg) {

            }

            @Override
            public void error(Marker marker, String format, Object arg) {

            }

            @Override
            public void error(Marker marker, String format, Object arg1, Object arg2) {

            }

            @Override
            public void error(Marker marker, String format, Object... arguments) {

            }

            @Override
            public void error(Marker marker, String msg, Throwable t) {

            }
        });

        return actorExecutor;
    }
}
