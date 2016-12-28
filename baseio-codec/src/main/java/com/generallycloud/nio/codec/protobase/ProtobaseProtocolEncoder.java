/*
 * Copyright 2015 GenerallyCloud.com
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
package com.generallycloud.nio.codec.protobase;

import java.io.IOException;
import java.nio.charset.Charset;

import com.generallycloud.nio.buffer.ByteBuf;
import com.generallycloud.nio.buffer.ByteBufAllocator;
import com.generallycloud.nio.buffer.EmptyByteBuf;
import com.generallycloud.nio.codec.protobase.future.ProtobaseReadFuture;
import com.generallycloud.nio.common.StringUtil;
import com.generallycloud.nio.component.BufferedOutputStream;
import com.generallycloud.nio.protocol.ChannelReadFuture;
import com.generallycloud.nio.protocol.ChannelWriteFuture;
import com.generallycloud.nio.protocol.ChannelWriteFutureImpl;
import com.generallycloud.nio.protocol.ProtocolEncoder;
import com.generallycloud.nio.protocol.ProtocolException;

public class ProtobaseProtocolEncoder implements ProtocolEncoder {

	private static final byte [] EMPTY_ARRAY = EmptyByteBuf.EMPTY_BYTEBUF.array();

	@Override
	public ChannelWriteFuture encode(ByteBufAllocator allocator, ChannelReadFuture readFuture) throws IOException {

		if (readFuture.isHeartbeat()) {

			byte b = (byte) (readFuture.isPING() ? 
					ProtobaseProtocolDecoder.PROTOCOL_PING
					: ProtobaseProtocolDecoder.PROTOCOL_PONG << 6);

			ByteBuf buf = allocator.allocate(1);

			buf.putByte(b);

			return new ChannelWriteFutureImpl(readFuture, buf.flip());
		}

		ProtobaseReadFuture f = (ProtobaseReadFuture) readFuture;

		String future_name = f.getFutureName();

		if (StringUtil.isNullOrBlank(future_name)) {
			throw new ProtocolException("future name is empty");
		}
		
		Charset charset = readFuture.getContext().getEncoding();

		byte[] future_name_array = future_name.getBytes(charset);
		
		if (future_name_array.length > 255) {
			throw new IllegalArgumentException("service name too long ," + future_name);
		}
		
		byte future_name_length = (byte) future_name_array.length;
		
		BufferedOutputStream binary = f.getWriteBinaryBuffer();
		
		String writeText = f.getWriteText();
		
		byte[] text_array;
		if (StringUtil.isNullOrBlank(writeText)) {
			text_array = EMPTY_ARRAY;
		}else{
			text_array = writeText.getBytes(charset);
		}
		
		if (binary != null) {
			return encode(allocator, f, future_name_array, text_array, binary);
		}
		
		int text_length = text_array.length;
		int header_length = ProtobaseProtocolDecoder.PROTOCOL_HEADER - 4;
		byte byte0 = 0x40;
		
		//0x40=01000000,0x60=01100000
		if (f.isBroadcast()) {
			byte0 = 0x60;
		}

		int all_length = header_length 
					+ future_name_length 
					+ text_length;

		ByteBuf buf = allocator.allocate(all_length);

		buf.putByte(byte0);
		buf.putByte(future_name_length);
		buf.putInt(f.getFutureID());
		buf.putInt(f.getSessionID());
		buf.putInt(f.getHashCode());
		buf.putUnsignedShort(text_length);

		buf.put(future_name_array);

		if (text_length > 0) {
			buf.put(text_array, 0, text_length);
		}

		return new ChannelWriteFutureImpl(readFuture, buf.flip());
	}
	
	private ChannelWriteFuture encode(ByteBufAllocator allocator, ProtobaseReadFuture f,byte[] future_name_array,byte[] text_array,BufferedOutputStream binary) throws IOException {

		byte future_name_length = (byte) future_name_array.length;
		int text_length = text_array.length;
		int header_length = ProtobaseProtocolDecoder.PROTOCOL_HEADER;
		int binary_length = binary.size();
		byte byte0 = 0x50;
		
		//0x50=01010000,0x70=01110000
		if (f.isBroadcast()) {
			byte0 = 0x70;
		}

		int all_length = header_length 
					+ future_name_length 
					+ text_length 
					+ binary_length;

		ByteBuf buf = allocator.allocate(all_length);

		buf.putByte(byte0);
		buf.putByte((byte) (future_name_length));
		buf.putInt(f.getFutureID());
		buf.putInt(f.getSessionID());
		buf.putInt(f.getHashCode());
		buf.putUnsignedShort(text_length);
		buf.putInt(binary_length);
		
		buf.put(future_name_array);

		if (text_length > 0) {
			buf.put(text_array, 0, text_length);
		}

		buf.put(binary.array(), 0, binary_length);

		return new ChannelWriteFutureImpl(f, buf.flip());
	}

}
