package ru.taskurotta.example.calculate.decider;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.Promise;
import ru.taskurotta.example.calculate.RandomException;
import ru.taskurotta.example.calculate.worker.client.MultiplierClient;
import ru.taskurotta.example.calculate.worker.client.NumberGeneratorClient;
import ru.taskurotta.example.calculate.worker.client.SummarizerClient;

public class MathActionDeciderImpl implements MathActionDecider {

    private NumberGeneratorClient numberGeneratorClient;
    private MultiplierClient multiplierClient;
    private SummarizerClient summarizerClient;
    private MathActionDeciderImpl selfAsync;
    private double errPossibility = 0.0d;
    private static final Logger logger = LoggerFactory.getLogger(MathActionDeciderImpl.class);

    @Override
    @Execute
    public void performAction() {

// No retries for decider
//        if(RandomException.isEventHappened(errPossibility)) {
//            logger.error("MathActionDecider: RANDOMLY FAILED!");
//            throw new RandomException("Its exception time");
//        }

        long start = System.currentTimeMillis();
        Promise<Integer> a = numberGeneratorClient.getNumber();

        selfAsync.callExecutor(a, start);

    }

    @Asynchronous
    public void callExecutor(Promise<Integer> a, long startTime) {

        if(RandomException.isEventHappened(errPossibility)) {
            throw new RandomException("Its exception time");
        }

        int oddOrEven = a.get()%2;//=0 чётное либо =1 нечётное.

        Promise<Integer> result = null;
        String action = "";
        switch(oddOrEven) {
        case 0:
            result = summarizerClient.summarize(a.get(), a.get());
            action = a.get()+"+"+a.get()+"=";
            break;
        case 1:
            result = multiplierClient.multiply(a.get(), a.get());
            action = a.get()+"*"+a.get()+"=";
            break;
        }

        selfAsync.logResult(result, action, startTime);

    }

    @Asynchronous
    public void logResult(Promise<Integer> result, String action, long startTime) {
        if(RandomException.isEventHappened(errPossibility)) {
            throw new RandomException("Its exception time");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SS");
        logger.info(sdf.format(new Date()) + ": " + action + result.get() + " in["+(System.currentTimeMillis()-startTime)+"]ms, started at["+sdf.format(new Date(startTime))+"]");
    }

    public void setNumberGeneratorClient(NumberGeneratorClient numberGeneratorClient) {
        this.numberGeneratorClient = numberGeneratorClient;
    }

    public void setMultiplierClient(MultiplierClient multiplierClient) {
        this.multiplierClient = multiplierClient;
    }

    public void setSummarizerClient(SummarizerClient summarizerClient) {
        this.summarizerClient = summarizerClient;
    }

    public void setSelfAsync(MathActionDeciderImpl selfAsync) {
        this.selfAsync = selfAsync;
    }

    public void setErrPossibility(double errPossibility) {
        this.errPossibility = errPossibility;
    }

}
