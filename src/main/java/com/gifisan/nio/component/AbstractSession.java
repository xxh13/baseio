package com.gifisan.nio.component;

import java.io.IOException;
import java.net.SocketException;

import com.gifisan.nio.Attachment;
import com.gifisan.nio.DisconnectException;
import com.gifisan.nio.common.CloseUtil;
import com.gifisan.nio.common.Logger;
import com.gifisan.nio.common.LoggerFactory;
import com.gifisan.nio.common.MessageFormatter;
import com.gifisan.nio.component.future.IOReadFuture;
import com.gifisan.nio.component.future.IOWriteFuture;
import com.gifisan.nio.component.future.ReadFuture;
import com.gifisan.nio.component.protocol.ProtocolEncoder;
import com.gifisan.nio.server.EmptyReadFuture;
import com.gifisan.nio.server.NIOContext;
import com.gifisan.nio.server.SessionFactory;

public abstract class AbstractSession extends AttributesImpl implements Session {

	private Attachment					attachment	= null;
	private Attachment[]				attachments	= new Attachment[4];
	private long						creationTime	= System.currentTimeMillis();
	private boolean					closed		= false;
	private String						machineType	= null;
	private SessionEventListenerWrapper	lastListener	= null;
	private SessionEventListenerWrapper	listenerStub	= null;
	protected TCPEndPoint				endPoint		= null;
	protected ProtocolEncoder			encoder		= null;
	protected EndPointWriter			endPointWriter	= null;
	protected String					sessionID		= null;
	private NIOContext					context		= null;
	private UDPEndPoint					udpEndPoint	= null;
	private Logger						logger		= LoggerFactory.getLogger(AbstractSession.class);

	public AbstractSession(TCPEndPoint endPoint) {
		this.context = endPoint.getContext();
		this.endPointWriter = endPoint.getEndPointWriter();
		this.encoder = context.getProtocolEncoder();
		this.endPoint = endPoint;
	}
	
	public UDPEndPoint getUDPEndPoint() {
		return udpEndPoint;
	}
	
	public void setUDPEndPoint(UDPEndPoint udpEndPoint) {

		if (this.udpEndPoint != null && this.udpEndPoint != udpEndPoint) {
			throw new IllegalArgumentException("udpEndPoint setted");
		}

		this.udpEndPoint = udpEndPoint;
	}

	public void disconnect() {
		this.endPoint.endConnect();
		this.endPoint.getEndPointWriter().offer(new EmptyReadFuture(endPoint));
	}

	public void flush(ReadFuture future) {

		IOReadFuture _future = (IOReadFuture) future;

		if (_future.flushed()) {
			throw new IllegalStateException("flushed already");
		}

		IOEventHandle handle = context.getIOEventHandle();

		if (!endPoint.isOpened()) {

			handle.exceptionCaughtOnWrite(this, future, null, DisconnectException.INSTANCE);

			return;
		}

		IOWriteFuture writeFuture = null;

		try {

			writeFuture = encoder.encode(endPoint, 0, _future.getServiceName(),
					_future.getTextCache().toByteArray(), _future.getInputStream());

			_future.flush();

			writeFuture.attach(_future.attachment());

			this.endPointWriter.offer(writeFuture);
		} catch (IOException e) {

			logger.debug(e.getMessage(), e);

			handle.exceptionCaughtOnWrite(this, future, writeFuture, e);
		}
	}

	public void addEventListener(SessionEventListener listener) {
		if (this.listenerStub == null) {
			this.listenerStub = new SessionEventListenerWrapper(listener);
			this.lastListener = this.listenerStub;
		} else {
			this.lastListener.setNext(new SessionEventListenerWrapper(listener));
		}
	}

	public NIOContext getContext() {
		return context;
	}

	public Attachment getAttachment() {
		return attachment;
	}

	public void setAttachment(Attachment attachment) {
		this.attachment = attachment;
	}

	public Attachment getAttachment(PluginContext context) {

		if (context == null) {
			throw new IllegalArgumentException("null context");
		}

		return attachments[context.getPluginIndex()];
	}

	public void setAttachment(PluginContext context, Attachment attachment) {

		if (context == null) {
			throw new IllegalArgumentException("null context");
		}

		this.attachments[context.getPluginIndex()] = attachment;
	}

	public long getCreationTime() {
		return this.creationTime;
	}

	public String getLocalAddr() {
		return endPoint.getLocalAddr();
	}

	public String getLocalHost() {
		return endPoint.getLocalHost();
	}

	public int getLocalPort() {
		return endPoint.getLocalPort();
	}

	public int getMaxIdleTime() throws SocketException {
		return endPoint.getMaxIdleTime();
	}

	public String getRemoteAddr() {
		return endPoint.getRemoteAddr();
	}

	public String getRemoteHost() {
		return endPoint.getRemoteHost();
	}

	public int getRemotePort() {
		return endPoint.getRemotePort();
	}

	public boolean isBlocking() {
		return endPoint.isBlocking();
	}

	public boolean isOpened() {
		return endPoint.isOpened();
	}

	public void destroy() {
		
		SessionFactory factory = context.getSessionFactory();

		factory.removeSession(this);

		CloseUtil.close(udpEndPoint);

		this.closed = true;

		SessionEventListenerWrapper listenerWrapper = this.listenerStub;

		for (; listenerWrapper != null;) {
			listenerWrapper.onDestroy(this);
			listenerWrapper = listenerWrapper.nextListener();
		}
	}

	protected TCPEndPoint getEndPoint() {
		return endPoint;
	}

	public String toString() {
		return MessageFormatter.format("session-{}@edp{}", this.getSessionID(), endPoint);
	}

	public boolean closed() {
		return closed;
	}

	public String getSessionID() {
		return sessionID;
	}

	public String getMachineType() {
		return machineType;
	}

	public void setMachineType(String machineType) {
		this.machineType = machineType;
	}

	public TCPEndPoint getTCPEndPoint() {
		return endPoint;
	}

}
