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
package com.generallycloud.baseio.component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;

import com.generallycloud.baseio.common.CloseUtil;
import com.generallycloud.baseio.component.concurrent.EventLoop;
import com.generallycloud.baseio.component.concurrent.ReentrantMap;

public class DatagramSessionManager extends AbstractSessionManager {

	private DatagramSelectorEventLoop						selectorLoop	= null;
	private DatagramChannelContext						context		= null;
	private ReentrantMap<InetSocketAddress, DatagramSession>	sessions		= new ReentrantMap<>();

	public DatagramSessionManager(DatagramChannelContext context) {
		super(context.getSessionIdleTime());
		this.context = context;
	}

	@Override
	protected void sessionIdle(long lastIdleTime, long currentTime) {

		Map<InetSocketAddress, DatagramSession> map = sessions.takeSnapshot();

		if (map.size() == 0) {
			return;
		}

		Collection<DatagramSession> es = map.values();

		DatagramChannelContext context = this.context;

		for (DatagramSession session : es) {

			sessionIdle(context, session, lastIdleTime, currentTime);
		}

	}

	protected void sessionIdle(DatagramChannelContext context, DatagramSession session,
			long lastIdleTime, long currentTime) {
		//FIXME rm session
	}

	@Override
	public void stop() {

		Map<InetSocketAddress, DatagramSession> map = sessions.takeSnapshot();

		if (map.size() == 0) {
			return;
		}

		Collection<DatagramSession> es = map.values();

		for (DatagramSession session : es) {

			CloseUtil.close(session);
		}
	}

	public void putSession(DatagramSession session) {

		ReentrantMap<InetSocketAddress, DatagramSession> sessions = this.sessions;

		InetSocketAddress remote = session.getRemoteSocketAddress();

		DatagramSession old = sessions.get(remote);

		if (old != null) {
			CloseUtil.close(old);
			removeSession(old);
		}

		sessions.put(remote, session);
	}

	public void removeSession(DatagramSession session) {
		sessions.remove(session.getRemoteSocketAddress());
	}

	public int getManagedSessionSize() {
		return sessions.size();
	}

	public DatagramSession getSession(InetSocketAddress sessionID) {
		return sessions.get(sessionID);
	}

	public void initSessionManager(EventLoop eventLoop) {
		this.selectorLoop = (DatagramSelectorEventLoop) eventLoop;
	}

	public DatagramSession getSession(java.nio.channels.DatagramChannel nioChannel, InetSocketAddress remote)
			throws IOException {

		DatagramSession session = sessions.get(remote);

		if (session == null) {

			@SuppressWarnings("resource")
			DatagramChannel channel = new NioDatagramChannel(selectorLoop, nioChannel, remote);

			session = channel.getSession();

			putSession(session);
		}

		return session;
	}

}