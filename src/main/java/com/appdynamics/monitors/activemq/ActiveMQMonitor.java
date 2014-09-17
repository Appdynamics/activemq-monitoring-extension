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

package com.appdynamics.monitors.activemq;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class ActiveMQMonitor extends AManagedMonitor
{
	private static final Logger LOG = Logger.getLogger("com.singularity.extensions.ActiveMQMonitor");
	private static final String metricPathPrefix = "Custom Metrics|ActiveMQ|";
	
	private Map<String, Object> brokerMetrics =  new HashMap<String, Object>();
	private Map<ObjectName, Map<String, Object>> queuesMap = new HashMap<ObjectName, Map<String,Object>>();
	private Map<ObjectName, Map<String, Object>> topicsMap = new HashMap<ObjectName, Map<String,Object>>();
	
	private Set<String> brokerExcludeMetrics = new HashSet<String>();
	private Set<String> queueExcludeMetrics = new HashSet<String>();
	private Set<String> topicExcludeMetrics = new HashSet<String>();
	
	private ActiveMQWrapper activeMQWrapper;
	
	/*
	 * Main execution method that uploads the metrics to AppDynamics Controller
	 * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
	 */
	public TaskOutput execute(Map<String, String> taskArguments,
			TaskExecutionContext arg1) throws TaskExecutionException
	{
		try
		{
			LOG.info("Starting ActiveMQ Monitoring Task");
			// Establish Connection with proper checks
			if(!taskArguments.containsKey("host") || !taskArguments.containsKey("port") || !taskArguments.containsKey("username") || !taskArguments.containsKey("password"))
			{
				LOG.error("Monitor.xml needs to contain all required task arguments");
				throw new RuntimeException("Monitor.xml needs to contain all required task arguments");
			}
			String host = taskArguments.get("host");
			String port = taskArguments.get("port");
			String userName = taskArguments.get("username");
			String password = taskArguments.get("password");
			String excludeCustomMetricsFile = taskArguments.get("exclude-metrics-path");
			String [] excludeQueues = taskArguments.get("exclude-queues").split(",");
			String [] excludeTopics = taskArguments.get("exclude-topics").split(",");
			
			// Set metrics to be shown on controller by excluding those metrics that are listed in metrics.xml
			initializeCustomMetrics(excludeCustomMetricsFile);
			
			activeMQWrapper = new ActiveMQWrapper();
			
			if(host != null && host !="" && port != null && port !="" && userName != null && password != null)
			{
				activeMQWrapper.connect(host, port, userName, password);
			} else 
			{
				LOG.error("Credentials null or empty in monitor.xml");
				throw new RuntimeException("Credentials null or empty in monitor.xml");
			}
			
			activeMQWrapper.initJMXPropertiesBasedOnVersion();
			
			String broker = activeMQWrapper.getBrokerNames();
			
			// Get map of mbeans with their metrics
			List<String> excludeQueuesList = Arrays.asList(excludeQueues);
			List<String> excludeTopicsList = Arrays.asList(excludeTopics);
			brokerMetrics = activeMQWrapper.getBrokerMetrics(broker);
			queuesMap = activeMQWrapper.getQueueMetrics(broker, excludeQueuesList);
			topicsMap = activeMQWrapper.getTopicMetrics(broker, excludeTopicsList);
			
			// Print Broker Metrics to Controller
			for (Map.Entry<String, Object> metric : brokerMetrics.entrySet())
			{
				if(!brokerExcludeMetrics.contains(metric.getKey()))
				printMetric(getMetricPrefix() + broker + "|", metric.getKey(), metric.getValue(), MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
			}
			
			// Print Queue Metrics to Controller
			for(Entry<ObjectName, Map<String, Object>> queue : queuesMap.entrySet())
			{
				ObjectName queueName = queue.getKey();
				Map<String, Object> metrics = queuesMap.get(queueName);
				for(Map.Entry<String, Object> queMetrics : metrics.entrySet())
				{
					if(!queueExcludeMetrics.contains(queMetrics.getKey()))
					printMetric(getMetricPrefix() + broker + "| Queue |" + queueName.getKeyProperty(activeMQWrapper.destinationName) + "|", queMetrics.getKey(), queMetrics.getValue(), MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
				}
			}
			
			// Print Topic Metrics to Controller
			for(Entry<ObjectName, Map<String, Object>> topic : topicsMap.entrySet())
			{
				ObjectName topicName = topic.getKey();
				Map<String, Object> metrics = topicsMap.get(topicName);
				for(Map.Entry<String, Object> topMetrics : metrics.entrySet())
				{
					if(!topicExcludeMetrics.contains(topMetrics.getKey()))
					printMetric(getMetricPrefix() + broker + "| Topic |" + topicName.getKeyProperty(activeMQWrapper.destinationName) + "|", topMetrics.getKey(), topMetrics.getValue(), MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
				}
			}
			LOG.info("ActiveMQ Metric Upload Complete");
			return new TaskOutput("ActiveMQ Metric Upload Complete");
		} catch (Exception e) {
			LOG.error("ActiveMQ Metric upload failed", e);
			return new TaskOutput("ActiveMQ Metric upload failed");
		} finally {
			try {
				activeMQWrapper.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}
	
	/**
	 * Returns the metric to AppDynamics Controller
	 * @param metricPath	Path where this metric can be viewed on Controller
	 * @param metricName	Name of the metric
	 * @param metricValue	Value of the metric
	 * @param aggregation	Specifies how the values reported during a one-minute period are aggregated (Average OR Observation OR Sum)
	 * @param timeRollup	specifies how the values are rolled up when converted from from one-minute granularity tables to 10-minute granularity and 60-minute granularity tables over time
	 * @param cluster		specifies how the metrics are aggregated in a tier (Collective OR Individual)
	 */
	private void printMetric(String metricPath, String metricName, Object metricValue, String aggregation, String timeRollup, String cluster)
    {
        MetricWriter metricWriter = super.getMetricWriter(metricPath + metricName, aggregation,
                timeRollup,
                cluster
        );
        if(metricValue != null) {
        	if (metricValue instanceof Double)
            {
                metricWriter.printMetric(String.valueOf(Math.round((Double) metricValue)));
            } else if (metricValue instanceof Float) {
                metricWriter.printMetric(String.valueOf(Math.round((Float) metricValue)));
            } else {
                metricWriter.printMetric(String.valueOf(metricValue));
            }
        }
    }
	
	/**
	 * Initializes which metrics are to be displayed from metrics.xml file
	 * @param excludeCustomMetricsFile	File with metrics that are to be excluded(metrics.xml) in conf directory
	 * @throws IOException
	 */
	private void initializeCustomMetrics(String excludeCustomMetricsFile) throws IOException
	{
		
		if(excludeCustomMetricsFile == null || excludeCustomMetricsFile.length() == 0)
		{
			return;
		}
		
		BufferedInputStream metricsFile = null;
		try
		{
			metricsFile = new BufferedInputStream(new FileInputStream(excludeCustomMetricsFile));
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(metricsFile);
			document.getDocumentElement().normalize();
			
			getMetricsFromXML(document, "broker-metrics", brokerExcludeMetrics);
			getMetricsFromXML(document, "queue-metrics", queueExcludeMetrics);
			getMetricsFromXML(document, "topic-metrics", topicExcludeMetrics);
		} catch (FileNotFoundException e)
		{
			LOG.error("Metrics file not found", e);
		} catch (ParserConfigurationException e)
		{
			LOG.error("Error in instantiating Document Builder", e);
		} catch (SAXException e)
		{
			LOG.error("Error in parsing file", e);
		} catch (IOException e)
		{
			LOG.error("Error", e);
		} finally
		{
			if(metricsFile != null)
				metricsFile.close();
		}
	}

	/**
	 * Reads the metrics from the configuration file
	 * @param document	Xml file object
	 * @param devMetrics	broker-metrics OR queue-metrics OR topic-metrics
	 * @param excludedMetrics	set of metrics that are to be excluded
	 */
	private void getMetricsFromXML(Document document, String devMetrics, Set<String> excludedMetrics)
	{
		Element item = (Element) document.getElementsByTagName(devMetrics).item(0);
		NodeList nList = item.getElementsByTagName("metric");
		for(int i=0; i < nList.getLength(); i++)
		{
			Node node = nList.item(i);
			excludedMetrics.add(node.getAttributes().getNamedItem("name").getTextContent());
		}
	}
	
	/**
	 * Returns metric path where metrics are observed in controller
	 * @return	metric path
	 */
	private String getMetricPrefix()
	{
		return metricPathPrefix;
	}
}
