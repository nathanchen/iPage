package com.github.zhongl.ex.page;

import com.github.zhongl.ex.codec.Codec;
import com.github.zhongl.ex.codec.ComposedCodecBuilder;
import com.github.zhongl.ex.codec.LengthCodec;
import com.github.zhongl.ex.codec.StringCodec;
import com.github.zhongl.ex.util.CallbackFuture;
import com.github.zhongl.util.FileTestContext;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class PageTest extends FileTestContext {

    private Page page;

    @Test
    public void get() throws Exception {
        dir = testDir("get");
        page = newPage();

        final String one = "1";

        CallbackFuture<Cursor> callback = new CallbackFuture<Cursor>();
        page.append(one, callback);
        page.force();

        assertThat((DefaultCursor) callback.get(), is(new DefaultCursor(0L, 5)));
    }

    @After
    public void tearDown() throws Exception {
        if (page != null) page.close();
    }

    private Page newPage() throws IOException {
        Codec codec = ComposedCodecBuilder.compose(new StringCodec())
                                          .with(LengthCodec.class)
                                          .build();
        return new Page(new File(dir, "0"), mock(Number.class), codec) {
            @Override
            protected boolean isOverflow() {
                return file().length() > 4096;
            }

            @Override
            protected Batch newBatch(int estimateBufferSize) {
                return new DefaultBatch(codec(), file().length(), estimateBufferSize);
            }
        };
    }
}
