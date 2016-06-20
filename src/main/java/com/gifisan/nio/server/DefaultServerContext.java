package com.gifisan.nio.server;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gifisan.nio.Encoding;
import com.gifisan.nio.common.InitializeUtil;
import com.gifisan.nio.common.LifeCycleUtil;
import com.gifisan.nio.common.Logger;
import com.gifisan.nio.common.LoggerFactory;
import com.gifisan.nio.common.SharedBundle;
import com.gifisan.nio.component.AbstractNIOContext;
import com.gifisan.nio.component.DatagramPacketAcceptor;
import com.gifisan.nio.component.DynamicClassLoader;
import com.gifisan.nio.component.LoginCenter;
import com.gifisan.nio.component.PluginContext;
import com.gifisan.nio.concurrent.ExecutorThreadPool;
import com.gifisan.nio.concurrent.ThreadPool;
import com.gifisan.nio.server.configuration.ApplicationConfiguration;
import com.gifisan.nio.server.configuration.ApplicationConfigurationLoader;
import com.gifisan.nio.server.configuration.FileSystemACLoader;
import com.gifisan.nio.server.configuration.ServerConfiguration;
import com.gifisan.nio.server.service.FilterService;
import com.gifisan.nio.server.service.GenericServlet;
import com.gifisan.nio.server.service.NIOFilter;
import com.gifisan.security.AuthorityLoginCenter;
import com.gifisan.security.RoleManager;

public class DefaultServerContext extends AbstractNIOContext implements ServerContext {

	private String						appLocalAddres		= null;
	private ApplicationConfiguration		configuration		= null;
	private ApplicationConfigurationLoader	configurationLoader	= new FileSystemACLoader();
	private FilterService				filterService		= null;
	private Logger						logger			= LoggerFactory.getLogger(DefaultServerContext.class);
	private LoginCenter					loginCenter		= new AuthorityLoginCenter();
	private List<NIOFilter>				pluginFilters		= new ArrayList<NIOFilter>();
	private Map<String, GenericServlet>	pluginServlets		= new HashMap<String, GenericServlet>();
	private SessionFactory				sessionFactory		= new SessionFactory();
	private NIOServer					server			= null;
	private ServerConfiguration			serverConfiguration	= null;
	private ThreadPool					serviceDispatcher	= null;
	private RoleManager					roleManager		= new RoleManager();
	private DynamicClassLoader			classLoader		= new DynamicClassLoader();

	public DefaultServerContext(NIOServer server) {
		this.server = server;
	}

	protected void doStart() throws Exception {
		SharedBundle bundle = SharedBundle.instance();

		this.configuration = configurationLoader.loadConfiguration(bundle);

		this.serverConfiguration = configuration.getServerConfiguration();

		int SERVER_CORE_SIZE = serverConfiguration.getSERVER_CORE_SIZE();

		Charset encoding = serverConfiguration.getSERVER_ENCODING();

		Encoding.DEFAULT = encoding;

		this.encoding = Encoding.DEFAULT;
		this.appLocalAddres = bundle.getBaseDIR() + "app/";
		this.serviceDispatcher = new ExecutorThreadPool("Service-Executor", SERVER_CORE_SIZE);
		this.readFutureAcceptor = new ServerReadFutureAcceptor(serviceDispatcher);
		this.protocolDecoder = new ServerProtocolDecoder();
		this.filterService = new FilterService(this, classLoader);
		this.outputStreamAcceptor = new ServerOutputStreamAcceptor(this);
		this.udpEndPointFactory = new ServerUDPEndPointFactory();

		logger.info("[NIOServer] ======================================= 服务开始启动 =======================================");
		logger.info("[NIOServer] 工作目录：  { {} }", appLocalAddres);
		logger.info("[NIOServer] 项目编码：  { {} }", encoding);
		logger.info("[NIOServer] 监听端口：  { {} }", serverConfiguration.getSERVER_PORT());
		logger.info("[NIOServer] 服务器核数：{ {} }", SERVER_CORE_SIZE);

		this.filterService.start();
		this.roleManager.initialize(this, null);
		this.loginCenter.initialize(this, null);
		this.serviceDispatcher.start();

	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	protected void doStop() throws Exception {
		LifeCycleUtil.stop(filterService);
		LifeCycleUtil.stop(serviceDispatcher);
		InitializeUtil.destroy(loginCenter, this, null);
	}

	public String getAppLocalAddress() {
		return appLocalAddres;
	}

	public ApplicationConfiguration getConfiguration() {
		return configuration;
	}

	public FilterService getFilterService() {
		return filterService;
	}

	public LoginCenter getLoginCenter() {
		return loginCenter;
	}

	public PluginContext getPluginContext(Class clazz) {

		PluginContext[] pluginContexts = filterService.getPluginContexts();

		for (PluginContext context : pluginContexts) {

			if (context == null) {
				continue;
			}

			if (context.getClass().isAssignableFrom(clazz)) {
				return context;
			}
		}
		return null;
	}

	public List<NIOFilter> getPluginFilters() {
		return pluginFilters;
	}

	public Map<String, GenericServlet> getPluginServlets() {
		return pluginServlets;
	}

	public NIOServer getServer() {
		return server;
	}

	public ServerConfiguration getServerConfiguration() {
		return serverConfiguration;
	}

	public boolean redeploy() {

		ApplicationConfiguration configuration;
		try {
			configuration = configurationLoader.loadConfiguration(SharedBundle.instance());
		} catch (Exception e) {
			logger.info(e.getMessage(), e);
			return false;
		}

		ServerConfiguration serverConfiguration = configuration.getServerConfiguration();

		if (serverConfiguration.getSERVER_PORT() != this.serverConfiguration.getSERVER_PORT()) {
			return false;
		}
		
		DynamicClassLoader classLoader = new DynamicClassLoader();

		boolean redeployed = filterService.redeploy(classLoader);

		if (redeployed) {

			this.configuration = configuration;

			this.serverConfiguration = serverConfiguration;
			
			this.classLoader = classLoader;
		}

		return redeployed;
	}

	public void setDatagramPacketAcceptor(DatagramPacketAcceptor datagramPacketAcceptor) {

		if (datagramPacketAcceptor == null) {
			throw new IllegalArgumentException("null");
		}

		if (this.datagramPacketAcceptor != null) {
			throw new IllegalArgumentException("already setted");
		}

		this.datagramPacketAcceptor = datagramPacketAcceptor;
	}

	public RoleManager getRoleManager() {
		return roleManager;
	}

	public void setLoginCenter(LoginCenter loginCenter) {

		if (loginCenter == null) {
			throw new IllegalArgumentException("null");
		}

		if (this.loginCenter.getClass() != AuthorityLoginCenter.class) {
			throw new IllegalArgumentException("already setted");
		}

		this.loginCenter = loginCenter;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}
	
}