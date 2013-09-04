import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: moroz
 * Date: 15.08.13
 * Time: 12:53
 * To change this template use File | Settings | File Templates.
 */
public class HZ3_Test {

    @Test
    public void test() {
        Config cfg = new Config();
        HazelcastInstance hz = Hazelcast.newHazelcastInstance(cfg);
        Map<String, Object> myLockedObject = hz.getMap("test");
        Lock lock = hz.getLock("test");
        lock.lock();
        try {
            myLockedObject.put("123", "2121321");
        } finally {
            lock.unlock();
        }
    }
}
