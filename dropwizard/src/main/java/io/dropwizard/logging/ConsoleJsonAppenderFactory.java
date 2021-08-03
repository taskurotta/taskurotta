package io.dropwizard.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Layout;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.validation.constraints.NotNull;
import java.util.TimeZone;

/**
 * An {@link AppenderFactory} implementation which provides an appender that writes events to the console in JSON format.
 * <p/>
 * <b>Configuration Parameters:</b>
 * <table>
 *     <tr>
 *         <td>Name</td>
 *         <td>Default</td>
 *         <td>Description</td>
 *     </tr>
 *     <tr>
 *         <td>{@code type}</td>
 *         <td><b>REQUIRED</b></td>
 *         <td>The appender type. Must be {@code console-json}.</td>
 *     </tr>
 *     <tr>
 *         <td>{@code threshold}</td>
 *         <td>{@code ALL}</td>
 *         <td>The lowest level of events to print to the console.</td>
 *     </tr>
 *     <tr>
 *         <td>{@code timeZone}</td>
 *         <td>{@code UTC}</td>
 *         <td>The time zone to which event timestamps will be converted.</td>
 *     </tr>
 *     <tr>
 *         <td>{@code target}</td>
 *         <td>{@code stdout}</td>
 *         <td>
 *             The name of the standard stream to which events will be written.
 *             Can be {@code stdout} or {@code stderr}.
 *         </td>
 *     </tr>
 * </table>
 *
 * @see AbstractAppenderFactory
 */
@JsonTypeName("console-json")
public class ConsoleJsonAppenderFactory extends AbstractAppenderFactory {
    @SuppressWarnings("UnusedDeclaration")
    public enum ConsoleStream {
        STDOUT("System.out"),
        STDERR("System.err");

        private final String value;

        ConsoleStream(String value) {
            this.value = value;
        }

        public String get() {
            return value;
        }
    }

    @NotNull
    private TimeZone timeZone = TimeZone.getTimeZone("UTC");

    @NotNull
    private ConsoleStream target = ConsoleStream.STDOUT;

    @JsonProperty
    public TimeZone getTimeZone() {
        return timeZone;
    }

    @JsonProperty
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    @JsonProperty
    public ConsoleStream getTarget() {
        return target;
    }

    @JsonProperty
    public void setTarget(ConsoleStream target) {
        this.target = target;
    }

    @Override
    public Appender<ILoggingEvent> build(LoggerContext context, String applicationName, Layout<ILoggingEvent> layout) {

        final ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setName("console-json-appender");
        appender.setContext(context);
        appender.setTarget(target.get());
        Layout<ILoggingEvent> jsonLayout = new JsonLayout();
        appender.setLayout(jsonLayout);
        jsonLayout.setContext(context);
        jsonLayout.start();
        addThresholdFilter(appender, threshold);
        appender.start();

        return wrapAsync(appender);
    }
}
