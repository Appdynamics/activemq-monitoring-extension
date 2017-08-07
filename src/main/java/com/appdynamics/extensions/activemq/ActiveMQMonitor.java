/**
 * Copyright 2013 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appdynamics.extensions.activemq;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.activemq.config.Configuration;
import com.appdynamics.extensions.activemq.config.Server;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.file.FileLoader;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.appdynamics.TaskInputArgs.ENCRYPTION_KEY;
import static com.appdynamics.TaskInputArgs.PASSWORD_ENCRYPTED;

public class ActiveMQMonitor extends AManagedMonitor {

	private static final Logger logger = LoggerFactory.getLogger(ActiveMQMonitor.class);
	public static final String CONFIG_ARG = "config-file";

	private ExecutorService executorService;
	private int executorServiceSize;
	private volatile boolean initialized;
	private Configuration config;

	public ActiveMQMonitor() {
		System.out.println(logVersion());
	}


	public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext out) throws TaskExecutionException {
		logVersion();
		try {
			initialize(taskArgs);
			//parallel execution for each server.
			runConcurrentTasks();
			logger.info("ActiveMQ monitor run completed successfully.");
			return new TaskOutput("ActiveMQ monitor run completed successfully.");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Metrics collection failed", e);
		}
		throw new TaskExecutionException();
	}

	private void initialize(Map<String, String> taskArgs) {
		if(!initialized){
			//read the config.
			final String configFilePath = taskArgs.get(CONFIG_ARG);
			File configFile = PathResolver.getFile(configFilePath, AManagedMonitor.class);
			if(configFile != null && configFile.exists()){
				FileLoader.load(new FileLoader.Listener() {
					public void load(File file) {
						String path = file.getAbsolutePath();
						try {
							if (path.contains(configFilePath)) {
								logger.info("The file [{}] has changed, reloading the config", file.getAbsolutePath());
								reloadConfig(file);
							}
							else {
								logger.warn("Unknown file [{}] changed, ignoring", file.getAbsolutePath());
							}
						} catch (Exception e) {
							logger.error("Exception while reloading the file " + file.getAbsolutePath(), e);
						}
					}
				}, configFilePath);
			}
			else{
				logger.error("Config file is not found.The config file path {} is resolved to {}",
						taskArgs.get(CONFIG_ARG), configFile != null ? configFile.getAbsolutePath() : null);
			}
			initialized = true;
		}
	}

	private void reloadConfig(File file) {
		config = YmlReader.readFromFile(file, Configuration.class);
		if (config != null) {
			int numOfThreads = config.getNumberOfThreads();
			if (executorService == null) {
				executorService = createThreadPool(numOfThreads);
				logger.info("Initializing the ThreadPool with size {}", config.getNumberOfThreads());
			}
			else if (numOfThreads != executorServiceSize) {
				logger.info("The ThreadPool size has been updated from {} -> {}", executorServiceSize, numOfThreads);
				executorService.shutdown();
				executorService = createThreadPool(numOfThreads);
			}
			executorServiceSize = numOfThreads;
			//decrypt password
			if(config.getEncryptionKey() != null){
					for(Server server : config.getServers()) {
						Map cryptoMap = Maps.newHashMap();
						cryptoMap.put(PASSWORD_ENCRYPTED,server.getEncryptedPassword());
						cryptoMap.put(ENCRYPTION_KEY,config.getEncryptionKey());
						server.setPassword(CryptoUtil.getPassword(cryptoMap));
					}
			}
		}
		else {
			throw new IllegalArgumentException("The config cannot be initialized from the file " + file.getAbsolutePath());
		}
	}


	private ExecutorService createThreadPool(int numOfThreads) {
		final ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat("ActiveMq-Task-Thread-%d")
				.build();
		return Executors.newFixedThreadPool(numOfThreads,
				threadFactory);
	}


	private void runConcurrentTasks() {
		if (config != null) {
			for (Server server : config.getServers()) {
				try {
					//passing the context to the task.
					ActiveMQMonitorTask task = createTask(server);
					executorService.execute(task);
				}
				catch (IOException e){
					logger.error("Unable to create JMX connection for {}",server.getDisplayName(),e);
				}
			}
		}
	}

	private ActiveMQMonitorTask createTask(Server server) throws IOException {
		return new ActiveMQMonitorTask.Builder()
				.metricPrefix(config.getMetricPrefix())
				.metricWriter(this)
				.serviceURL(createJMXServiceUrl(server))
				.server(server)
				.mbeans(config.getMbeans())
				.build();
	}



	private JMXServiceURL createJMXServiceUrl(Server server) throws MalformedURLException {
		String url;
		if(Strings.isNullOrEmpty(server.getServiceUrl())) {
			url = "service:jmx:rmi:///jndi/rmi://" + server.getHost() + ":" + server.getPort() + "/jmxrmi";
		}
		else{
			url = server.getServiceUrl();
		}
		return new JMXServiceURL(url);
	}


	private static String getImplementationVersion() {
		return ActiveMQMonitor.class.getPackage().getImplementationTitle();
	}


	private String logVersion() {
		String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		return msg;
	}
	public static void main (String[] args) throws TaskExecutionException {
		ActiveMQMonitor activeMQMonitor = new ActiveMQMonitor();

		Map<String, String> argsMap = new HashMap<String, String>();
		argsMap.put("config-file", "/Users/bhuvnesh.kumar/repos/appdynamics/extensions/activemq-monitoring-extension" +
				"" + "" + "/src/main/resources/conf/config.yml");
		activeMQMonitor.execute(argsMap, null);
	}

}
