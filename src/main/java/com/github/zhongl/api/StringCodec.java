/*
 * Copyright 2012 zhongl
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

package com.github.zhongl.api;


import com.github.zhongl.codec.Codec;

import java.nio.ByteBuffer;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class StringCodec implements Codec<String> {
    @Override
    public String decode(ByteBuffer buffer) {
        int length = buffer.remaining();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes);
    }

    @Override
    public ByteBuffer encode(String object) {
        return ByteBuffer.wrap(object.getBytes());
    }
}
