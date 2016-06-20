package com.gifisan.nio.server;

import java.io.IOException;

import com.gifisan.nio.component.TCPEndPoint;
import com.gifisan.nio.component.future.AbstractWriteFuture;
import com.gifisan.nio.component.future.IOWriteFuture;

public class EmptyReadFuture extends AbstractWriteFuture implements IOWriteFuture{

	public EmptyReadFuture(TCPEndPoint endPoint) {
		super(endPoint, null, 0, null, null, null);
	}

	public boolean write() throws IOException {
		return false;
	}
}