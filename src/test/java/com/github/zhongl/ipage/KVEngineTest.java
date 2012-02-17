package com.github.zhongl.ipage;

import com.github.zhongl.util.FileTestContext;
import com.github.zhongl.util.Md5;
import org.junit.Test;

import static com.github.zhongl.ipage.KVEngine.QoS;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class KVEngineTest extends FileTestContext {

    @Test
    public void latency() throws Exception {
        dir = testDir("latency");

        KVEngine<String, byte[]> engine = new KVEngine<String, byte[]>(
                new IPage<String, byte[]>(dir, new BytesCodec(), 10, 1000L, 1) {

                    @Override
                    protected Key transform(String key) {
                        return new Key(Md5.md5(key.getBytes()));
                    }
                }, QoS.LATENCY);

        String key = "key";
        byte[] value = key.getBytes();

        engine.add(key, value);
        assertThat(engine.get(key), is(value));

        assertThat(engine.iterator().hasNext(), is(false));

        engine.remove(key);
        assertThat(engine.get(key), is(nullValue()));

        engine.stop();
    }

    @Test
    public void reliable() throws Exception {
        dir = testDir("reliable");

        KVEngine<String, byte[]> engine = new KVEngine<String, byte[]>(
                new IPage<String, byte[]>(dir, new BytesCodec(), 10, 1000L, 1) {

                    @Override
                    protected Key transform(String key) {
                        return new Key(Md5.md5(key.getBytes()));
                    }
                }, QoS.RELIABLE);

        String key = "key";
        byte[] value = key.getBytes();

        engine.add(key, value);
        assertThat(engine.get(key), is(value));

        assertThat(engine.iterator().next(), is(value));

        engine.remove(key);
        assertThat(engine.get(key), is(nullValue()));

        engine.stop();
    }

}
