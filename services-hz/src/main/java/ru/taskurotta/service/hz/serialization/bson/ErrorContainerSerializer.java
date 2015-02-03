package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.ErrorContainer;

/**
 * Created by greg on 03/02/15.
 */
public class ErrorContainerSerializer implements StreamBSerializer<ErrorContainer> {

    private CString MESSAGE = new CString("message");
    private CString CLASSNAMES = new CString("classNames");
    private CString STACK = new CString("stack");

    @Override
    public void write(BDataOutput out, ErrorContainer object) {
        out.writeString(MESSAGE, object.getMessage());
        int arrayClassNamesLabel = out.writeArray(CLASSNAMES);
        for (int i = 0; i < object.getClassNames().length; i++) {
            String cl = object.getClassNames()[i];
            out.writeString(i, cl);
        }
        out.writeArrayStop(arrayClassNamesLabel);
        out.writeString(STACK, object.getStackTrace());
    }

    @Override
    public ErrorContainer read(BDataInput in) {
        ErrorContainer errorContainer = new ErrorContainer();
        errorContainer.setMessage(in.readString(MESSAGE));
        int arrayClassNamesLabel = in.readArray(CLASSNAMES);
        int arrayCNsize = in.readArraySize();
        String[] strings = new String[arrayCNsize];
        for (int i = 0; i< arrayCNsize; i++){
           strings[i] = in.readString(i);
        }
        in.readArrayStop(arrayClassNamesLabel);
        errorContainer.setClassNames(strings);
        errorContainer.setStackTrace(in.readString(STACK));
        return errorContainer;
    }
}
