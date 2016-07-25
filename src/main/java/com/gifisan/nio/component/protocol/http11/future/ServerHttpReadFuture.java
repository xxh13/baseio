package com.gifisan.nio.component.protocol.http11.future;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import com.gifisan.nio.common.StringUtil;
import com.gifisan.nio.component.BufferedOutputStream;
import com.gifisan.nio.component.IOEventHandleAdaptor;
import com.gifisan.nio.component.Session;

public class ServerHttpReadFuture extends AbstractHttpReadFuture implements HttpReadFuture {

	public ServerHttpReadFuture(Session session, HttpHeaderParser httpHeaderParser, ByteBuffer readBuffer) {
		super(session, httpHeaderParser, readBuffer);
		params = new HashMap<String, String>();
	}

	protected void decodeHeader(byte[] source_array, int length, int pos) throws IOException {

		int index = requestURI.indexOf("?");

		if (index > -1) {
			String paramString = requestURI.substring(index + 1, requestURI.length());

			parseParamString(paramString);

			requestURI = requestURI.substring(0, index);
		}

		if (contentLength < 1) {

			body_complete = true;

		} else if (contentLength > 1 << 21) {
			
			this.setHasOutputStream(true);

			this.body_buffer = ByteBuffer.allocate(1024 * 256);

			IOEventHandleAdaptor eventHandle = session.getContext().getIOEventHandleAdaptor();

			try {
				eventHandle.acceptAlong(session, this);
			} catch (Exception e) {
				throw new IOException(e.getMessage(),e);
			}

			if (this.outputStream == null) {

				throw new IOException("none outputstream");
			}

			read_length = length - pos;

			outputStream.write(source_array, pos, read_length);

		} else {

			outputStream = new BufferedOutputStream(contentLength);

			read_length = length - pos;

			outputStream.write(source_array, pos, read_length);
		}
	}

	protected void decodeBody() {

		BufferedOutputStream o = (BufferedOutputStream) outputStream;

		if (!hasOutputStream() && o.size() > 0) {

			String paramString = new String(o.toByteArray(), session.getContext().getEncoding());

			parseParamString(paramString);
		}

		body_complete = true;

	}

	private void parseParamString(String paramString) {
		String[] array = paramString.split("&");
		for (String s : array) {

			if (StringUtil.isNullOrBlank(s)) {
				continue;
			}

			String[] unitArray = s.split("=");

			if (unitArray.length != 2) {
				continue;
			}

			String key = unitArray[0];
			String value = unitArray[1];
			params.put(key, value);
		}
	}
	
	public void setHeader(String name, String value) {
		if (response_headers == null) {
			response_headers = new HashMap<String, String>();
			request_headers.put("content-Type", "text/plain");
		}
		response_headers.put(name, value);
	}
}
