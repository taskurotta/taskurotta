package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.ErrorContainer;

import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readArrayOfString;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeArrayOfString;

public class ErrorContainerBSerializer implements StreamBSerializer<ErrorContainer> {

    private CString MESSAGE = new CString("message");
    private CString CLASS_NAMES = new CString("className");
    private CString STACK = new CString("stackTrace");

    @Override
    public Class<ErrorContainer> getObjectClass() {
        return ErrorContainer.class;
    }

    @Override
    public void write(BDataOutput out, ErrorContainer object) {
        writeArrayOfString(CLASS_NAMES, object.getClassNames(), out);
        out.writeString(MESSAGE, object.getMessage());
        out.writeString(STACK, object.getStackTrace());
    }

    @Override
    public ErrorContainer read(BDataInput in) {

        String[] classNames = readArrayOfString(CLASS_NAMES, in);
        String message = in.readString(MESSAGE);
        String stackTrace = in.readString(STACK);

        return new ErrorContainer(classNames, message, stackTrace);
    }
}
