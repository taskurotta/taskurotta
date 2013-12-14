package com.hazelcast.nio.serialization;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import org.junit.Test;
import ru.taskurotta.hazelcast.util.ConfigUtil;

import java.io.IOException;
import java.util.TreeSet;

/**
 * User: romario
 * Date: 12/2/13
 * Time: 10:43 AM
 */
public class SerializationServiceImplTest {

    public class SimpleObject {
        public int aInt = 10;
        public boolean aBoolean = true;
    }

    public class SimpleObjectStreamSerializer implements StreamSerializer<SimpleObject> {

        @Override
        public void write(ObjectDataOutput out, SimpleObject object) throws IOException {
            out.writeInt(object.aInt);
            out.writeBoolean(object.aBoolean);
        }

        @Override
        public SimpleObject read(ObjectDataInput in) throws IOException {
            SimpleObject object = new SimpleObject();
            object.aInt = in.readInt();
            object.aBoolean = in.readBoolean();
            return object;
        }

        @Override
        public int getTypeId() {
            return 1;
        }

        @Override
        public void destroy() {
        }
    }

    //@Test
    public void registeredStreamSerializerTest() {
       HazelcastInstance hzInstance = Hazelcast.newHazelcastInstance(ConfigUtil.disableMulticast(new Config()));

        try {
            IMap map = hzInstance.getMap("test");
            map.set("gg", new SimpleObject());
        } catch (IllegalStateException e) {
            return;
        } finally {
            hzInstance.shutdown();
        }

        throw new IllegalStateException("Exception not carched");

    }

}
