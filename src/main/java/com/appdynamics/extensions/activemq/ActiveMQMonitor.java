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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.activemq.config.ConfigUtil;
import com.appdynamics.extensions.activemq.config.Configuration;
import com.appdynamics.extensions.activemq.config.Server;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class ActiveMQMonitor extends AManagedMonitor {

	private static final Logger logger = Logger.getLogger("com.singularity.extensions.ActiveMQMonitor");
	public static final String CONFIG_ARG = "config-file";
	public static final String METRIC_SEPARATOR = "|";
	private static final int DEFAULT_NUMBER_OF_THREADS = 10;
	public static final int DEFAULT_THREAD_TIMEOUT = 10;

	private ExecutorService threadPool;

	// To load the config files
	private final static ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();

	public ActiveMQMonitor() {
		String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		System.out.println(msg);
	}

	/*
	 * Main execution method that uploads the metrics to AppDynamics Controller
	 * 
	 * @see
	 * com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map,
	 * com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
	 */
	public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext arg1) throws TaskExecutionException {
		if (taskArguments != null) {
			logger.info("Starting ActiveMQ Monitoring Task");
			if (logger.isDebugEnabled()) {
				logger.debug("Task Arguments Passed ::" + taskArguments);
			}
			String configFilename = getConfigFilename(taskArguments.get(CONFIG_ARG));
			try {
				// read config file
				Configuration config = configUtil.readConfig(configFilename, Configuration.class);
				threadPool = Executors.newFixedThreadPool(config.getNumberOfThreads() == 0 ? DEFAULT_NUMBER_OF_THREADS : config.getNumberOfThreads());
				List<Future<ActiveMQMetrics>> parallelTasks = createConcurrentTasks(config);
				List<ActiveMQMetrics> aMetrics = collectMetrics(parallelTasks,
						config.getThreadTimeout() == 0 ? DEFAULT_THREAD_TIMEOUT : config.getThreadTimeout());
				printStats(config, aMetrics);
				logger.info("ActiveMQ Monitoring Task completed successfully");
				return new TaskOutput("ActiveMQ Monitoring Task completed successfully");
			} catch (FileNotFoundException e) {
				logger.error("Config file not found :: " + configFilename + " ", e);
			} catch (Exception e) {
				logger.error("Metrics collection failed ", e);
			} finally {
				if (!threadPool.isShutdown()) {
					threadPool.shutdown();
				}
			}
		}
		throw new TaskExecutionException("ActiveMQ monitoring task completed with failures.");
	}

	private List<ActiveMQMetrics> collectMetrics(List<Future<ActiveMQMetrics>> parallelTasks, int timeout) {
		List<ActiveMQMetrics> allMetrics = new ArrayList<ActiveMQMetrics>();
		for (Future<ActiveMQMetrics> aParallelTask : parallelTasks) {
			ActiveMQMetrics aMetric = null;
			try {
				aMetric = aParallelTask.get(timeout, TimeUnit.SECONDS);
				allMetrics.add(aMetric);
			} catch (InterruptedException e) {
				logger.error("Task interrupted. ", e);
			} catch (ExecutionException e) {
				logger.error("Task execution failed. ", e);
			} catch (TimeoutException e) {
				logger.error("Task timed out. ", e);
			}
		}
		return allMetrics;
	}

	private List<Future<ActiveMQMetrics>> createConcurrentTasks(Configuration config) {
		List<Future<ActiveMQMetrics>> parallelTasks = Lists.newArrayList();
		if (config != null && config.getServers() != null) {
			for (Server server : config.getServers()) {
				ActiveMQMonitorTask activeMQMonitorTask = new ActiveMQMonitorTask(server, config.getMbeans());
				parallelTasks.add(getThreadPool().submit(activeMQMonitorTask));
			}
		}
		return parallelTasks;
	}

	private void printStats(Configuration config, List<ActiveMQMetrics> aMetrics) {
		for (ActiveMQMetrics aMetric : aMetrics) {
			StringBuilder metricPath = new StringBuilder();
			metricPath.append(config.getMetricPrefix()).append(aMetric.getDisplayName()).append(METRIC_SEPARATOR);
			Map<String, String> metricsForAServer = aMetric.getMetrics();
			for (Map.Entry<String, String> entry : metricsForAServer.entrySet()) {
				printAverageAverageIndividual(metricPath.toString() + entry.getKey(), entry.getValue());
			}
		}
	}

	private void printAverageAverageIndividual(String metricPath, String metricValue) {
		printMetric(metricPath, metricValue, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
	}

	/**
	 * Returns the metric to AppDynamics Controller
	 * 
	 * @param metricPath
	 *            Path where this metric can be viewed on Controller
	 * @param metricName
	 *            Name of the metric
	 * @param metricValue
	 *            Value of the metric
	 * @param aggregation
	 *            Specifies how the values reported during a one-minute period
	 *            are aggregated (Average OR Observation OR Sum)
	 * @param timeRollup
	 *            specifies how the values are rolled up when converted from
	 *            from one-minute granularity tables to 10-minute granularity
	 *            and 60-minute granularity tables over time
	 * @param cluster
	 *            specifies how the metrics are aggregated in a tier (Collective
	 *            OR Individual)
	 */
	private void printMetric(String metricPath, String metricValue, String aggregation, String timeRollup, String cluster) {
		MetricWriter metricWriter = super.getMetricWriter(metricPath, aggregation, timeRollup, cluster);
		if (metricValue != null) {
			metricWriter.printMetric(metricValue);
		}
	}

	public ExecutorService getThreadPool() {
		return threadPool;
	}

	/**
	 * Returns a config file name,
	 * 
	 * @param filename
	 * @return String
	 */
	private String getConfigFilename(String filename) {
		if (filename == null) {
			return "";
		}
		// for absolute paths
		if (new File(filename).exists()) {
			return filename;
		}
		// for relative paths
		File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
		String configFileName = "";
		if (!Strings.isNullOrEmpty(filename)) {
			configFileName = jarPath + File.separator + filename;
		}
		return configFileName;
	}

	private static String getImplementationVersion() {
		return ActiveMQMonitor.class.getPackage().getImplementationTitle();
	}
}
