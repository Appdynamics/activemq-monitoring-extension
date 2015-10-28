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
import com.appdynamics.extensions.jmx.JMXConnectionConfig;
import com.appdynamics.extensions.jmx.JMXConnectionUtil;
import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ActiveMQMonitor extends AManagedMonitor {

	private static final Logger logger = Logger.getLogger(ActiveMQMonitor.class);
	public static final String CONFIG_ARG = "config-file";
	private static final int DEFAULT_NUMBER_OF_THREADS = 10;
	public static final int DEFAULT_THREAD_TIMEOUT = 10;

	private ExecutorService threadPool;

	public ActiveMQMonitor() {
		System.out.println(logVersion());
	}

	/*
	 * Main execution method that uploads the metrics to AppDynamics Controller
	 * 
	 * @see
	 * com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map,
	 * com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
	 */
	public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext arg1) throws TaskExecutionException {
		logVersion();
		if (taskArgs != null) {
			logger.info("Starting the ActiveMQ Monitoring task.");
			if (logger.isDebugEnabled()) {
				logger.debug("Task Arguments Passed ::" + taskArgs);
			}

			try {
				//read the config.
				File configFile = PathResolver.getFile(taskArgs.get(CONFIG_ARG), AManagedMonitor.class);
				Configuration config = YmlReader.readFromFile(configFile, Configuration.class);
				threadPool = Executors.newFixedThreadPool(config.getNumberOfThreads() == 0 ? DEFAULT_NUMBER_OF_THREADS : config.getNumberOfThreads());
				//parallel execution for each server.
				runConcurrentTasks(config);
				logger.info("ActiveMQ monitoring task completed successfully.");
				return new TaskOutput("ActiveMQ monitoring task completed successfully.");
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Metrics collection failed", e);
			} finally {
				if(!threadPool.isShutdown()){
					threadPool.shutdown();
				}
			}

		}
		throw new TaskExecutionException("ActiveMQ monitoring task completed with failures.");
	}


	/**
	 * Executes concurrent tasks
	 *
	 * @param config
	 * @return Handles to concurrent tasks.
	 */
	private void runConcurrentTasks(Configuration config) {
		List<Future<Void>> parallelTasks = new ArrayList<Future<Void>>();
		if (config != null && config.getServers() != null) {
			for (Server server : config.getServers()) {
				MetricOverride[] metricOverrides = (server.getMetricOverrides() != null) ? server.getMetricOverrides() : config.getMetricOverrides();
				JMXConnectionUtil jmxConnector = createJMXConnector(server);
				//passing the context to the task.
				ActiveMQMonitorTask activeMQTask = new ActiveMQMonitorTask(config.getMetricPrefix(),server.getDisplayName(),metricOverrides,jmxConnector,this);
				parallelTasks.add(threadPool.submit(activeMQTask));
			}
		}
		for (Future<Void> aTask : parallelTasks) {
			try {
				aTask.get(config.getThreadTimeout(), TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				logger.error("Task interrupted.", e);
			} catch (ExecutionException e) {
				logger.error("Task execution failed.", e);
			} catch (TimeoutException e) {
				logger.error("Task timed out.",e);
			} catch (Exception e){
				logger.error("Unknown exception in thread", e);
			}
		}
	}

	private JMXConnectionUtil createJMXConnector(Server server) {
		JMXConnectionUtil jmxConnector = null;
		if(Strings.isNullOrEmpty(server.getServiceUrl())) {
			jmxConnector = new JMXConnectionUtil(new JMXConnectionConfig(server.getHost(), server.getPort(), server.getUsername(), server.getPassword()));
		}
		else {
			jmxConnector = new JMXConnectionUtil(new JMXConnectionConfig(server.getServiceUrl(), server.getUsername(), server.getPassword()));
		}
		return jmxConnector;
	}


	private static String getImplementationVersion() {
		return ActiveMQMonitor.class.getPackage().getImplementationTitle();
	}


	private String logVersion() {
		String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		return msg;
	}
}
