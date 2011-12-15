/*
 * Copyright 2011 zhongl
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.zhongl.ipage;

import com.github.zhongl.buffer.Accessor;
import com.github.zhongl.builder.*;
import com.github.zhongl.integerity.ValidateOrRecover;
import com.github.zhongl.integerity.Validator;
import com.github.zhongl.util.FileHandler;
import com.github.zhongl.util.NumberNamedFilesLoader;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.ArrayList;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
@NotThreadSafe
public class IPage<T> implements Closeable, ValidateOrRecover<T, IOException> {

    private final ChunkList<T> chunkList;
    private final GarbageCollector<T> garbageCollector;
    private final ChunkFactory<T> chunkFactory;

    public static Builder baseOn(File dir) {
        Builder builder = Builders.newInstanceOf(Builder.class);
        builder.dir(dir);
        return builder;
    }

    IPage(File baseDir,
          Accessor<T> accessor,
          int minimizeChunkCapacity,
          long minimizeCollectLength,
          long maxChunkIdleTimeMillis) throws IOException {
        this.chunkFactory = new ChunkFactory<T>(baseDir, accessor, minimizeChunkCapacity, maxChunkIdleTimeMillis);
        this.chunkList = new ChunkList<T>(loadExistChunksBy(baseDir, chunkFactory));
        garbageCollector = new GarbageCollector<T>(chunkList, minimizeCollectLength);
    }

    private ArrayList<Chunk<T>> loadExistChunksBy(File baseDir, final ChunkFactory<T> chunkFactory) throws IOException {
        return new NumberNamedFilesLoader<Chunk<T>>(baseDir, new FileHandler<Chunk<T>>() {
            @Override
            public Chunk<T> handle(File file, boolean last) throws IOException {
                return last ? chunkFactory.appendableChunkOn(file) : chunkFactory.readOnlyChunkOn(file);
            }
        }).loadTo(new ArrayList<Chunk<T>>());
    }

    public Chunk<T> grow() throws IOException {
        Chunk<T> chunk;
        if (chunkList.isEmpty()) chunk = chunkFactory.newFirstAppendableChunk();
        else {
            chunk = chunkFactory.newAppendableAfter(chunkList.last());
            convertLastRecentlyUsedChunkToReadOnly();
        }
        chunkList.append(chunk);
        return chunk;
    }

    private void convertLastRecentlyUsedChunkToReadOnly() throws IOException {
        chunkList.set(chunkList.lastIndex(), chunkList.last().asReadOnly());
    }

    public long append(T record) throws IOException {
        try {
            return chunkList.last().append(record);
        } catch (IndexOutOfBoundsException e) { // empty
        } catch (BufferOverflowException e) { } // chunk no space for appending
        grow();
        return append(record);
    }

    public T get(long offset) throws IOException {
        try { return chunkList.chunkIn(offset).get(offset); } catch (IndexOutOfBoundsException e) { return null; }
    }

    public Cursor<T> next(Cursor<T> cursor) throws IOException {
        try {
            long beginPosition = chunkList.first().beginPosition();
            if (cursor.offset() < beginPosition) cursor = Cursor.begin(beginPosition);
            return chunkList.chunkIn(cursor.offset()).next(cursor);
        } catch (IndexOutOfBoundsException e) {
            return cursor.end();
        }
    }

    public long garbageCollect(long survivorOffset) throws IOException {
        return garbageCollector.collect(survivorOffset);
    }

    public void flush() throws IOException { chunkList.last().flush(); }

    @Override
    public boolean validateOrRecoverBy(Validator<T, IOException> validator) throws IOException {
        return chunkList.last().validateOrRecoverBy(validator);
    }

    @Override
    public void close() throws IOException { chunkList.close(); }

    public static interface Builder<T> extends BuilderConvention {

        @NotNull
        Builder<T> dir(File dir);

        @NotNull
        Builder<T> accessor(Accessor<T> value);

        @DefaultValue("4096")
        @GreaterThanOrEqual("4096")
        Builder<T> minimizeChunkCapacity(int value);

        @DefaultValue("4096")
        @GreaterThanOrEqual("4096")
        Builder<T> minimizeCollectLength(int value);

        @DefaultValue("4000")
        @GreaterThanOrEqual("1000")
        Builder<T> maxChunkIdleTimeMillis(long value);

        IPage<T> build();

    }
}
