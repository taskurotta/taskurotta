package ru.taskurotta.proxy;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.Promise;

import java.lang.reflect.Method;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * User: romario
 * Date: 1/18/13
 * Time: 1:23 PM
 */
public class CGLIBTest {
	protected static final Logger logger = LoggerFactory.getLogger(CGLIBTest.class);

    private final static String FIRST_NAME = "Sergo";
    private final static String SECOND_NAME = "Pupko";
    private final static String FULL_NAME = FIRST_NAME + " " + SECOND_NAME;


    @Decider
    public static interface SimpleDecider {

        @Execute
        public Promise<String> start(String fullName);

    }

    public static class TestDeciderImpl implements SimpleDecider {

		public TestDeciderImpl async;

        @Override
        public Promise<String> start(String fullName) {
			Promise<String> str = Promise.asPromise(fullName);
            return async.getFirstUserName(str);
        }

        @Asynchronous
        protected Promise<String> getFirstUserName(Promise<String> fullNamePromise) {
			String fullName = fullNamePromise.get();

			logger.trace("getFirstUserName {}", fullName);

            String result = fullName.substring(0, fullName.indexOf(' '));

            return Promise.asPromise(result);
        }
    }

    @Test
    public void createObjectProxy() {

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(TestDeciderImpl.class);

        Callback taskCallback =  new MethodInterceptor () {

            @Override
            public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
                logger.trace("Method intercepted! : {} args: {}", method.getName(), Arrays.toString(args));

                return methodProxy.invokeSuper(object, args);
            }
        };

        Callback disallowCallback =  new MethodInterceptor () {

            @Override
            public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {

                throw new IllegalAccessError(
                        "Access denied to methods witch has no Asynchronous annotation: "
                                + object.getClass().getName() + "." + method.getName() + "()");
            }
        };

        Callback[] callbacks = new Callback[]{taskCallback, disallowCallback};
        enhancer.setCallbacks(callbacks);

        CallbackFilter callbackFilter = new CallbackFilter() {

            private static final int INTERCEPT_TASK = 0;
            private static final int THROW_EXCEPTION = 1;

            @Override
            public int accept(Method method) {

				logger.trace("asynchronousAnnotation: {}", method.getName());

                Asynchronous asynchAnnotation = method.getAnnotation(Asynchronous.class);

                if (asynchAnnotation != null) {
                    return INTERCEPT_TASK;
                }

                return THROW_EXCEPTION;
            }
        };

        enhancer.setCallbackFilter(callbackFilter);

        TestDeciderImpl origDeciderImpl = new TestDeciderImpl();

        TestDeciderImpl asyncDeciderImpl = (TestDeciderImpl) enhancer.create();
        origDeciderImpl.async = asyncDeciderImpl;

        Promise<String> result = origDeciderImpl.start(FULL_NAME);
        assertEquals(result.get(), FIRST_NAME);

        // Check deny access to methods which has no Asynchronous annotation
        IllegalAccessError illegalAccessError = null;

        try {
            asyncDeciderImpl.toString();
        } catch (IllegalAccessError ex) {
            System.err.println(ex.getMessage());
            illegalAccessError = ex;
        }

        assertNotNull(illegalAccessError);
    }
}
