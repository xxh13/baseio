/*
 * Copyright 2015-2017 GenerallyCloud.com
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.generallycloud.baseio.protocol;

import java.io.IOException;
import java.nio.charset.Charset;

import com.generallycloud.baseio.buffer.ByteBuf;
import com.generallycloud.baseio.component.ChannelContext;
import com.generallycloud.baseio.component.NioEventLoop;
import com.generallycloud.baseio.component.NioSocketChannel;
import com.generallycloud.baseio.concurrent.Linkable;

public interface Future extends Linkable {

    Future duplicate();

    Future flush();

    boolean flushed();

    ByteBuf getByteBuf();

    int getByteBufLimit();

    byte[] getWriteBuffer();

    int getWriteSize();

    boolean isHeartbeat();

    boolean isNeedSsl();

    boolean isPING();

    boolean isPONG();

    boolean isReleased();

    boolean isSilent();

    boolean isWriteCompleted();

    /**
     * return true if the future read complete
     * @param channel
     * @param src
     * @return
     * @throws IOException
     */
    boolean read(NioSocketChannel channel, ByteBuf src) throws IOException;

    void release(NioEventLoop loop);

    void setByteBuf(ByteBuf buf);

    void setHeartbeat(boolean isPing);

    void setNeedSsl(boolean needSsl);

    Future setPING();

    Future setPONG();

    void setSilent(boolean isSilent);

    void write(byte b);

    void write(byte b[]);

    void write(byte b[], int off, int len);

    void write(String text, ChannelContext context);

    void write(String text, Charset charset);

    void write(String text, NioSocketChannel channel);

}
