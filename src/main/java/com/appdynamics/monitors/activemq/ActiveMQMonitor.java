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
	private static final Logger LOG = Logger.getLogger(ActiveMQMonitor.class);
	private static final String metricPathPrefix = "Custom Metics|ActiveMQ|";
	
	private Map<String, Object> brokerMetrics =  new HashMap<String, Object>();
	private Map<ObjectName, Map<String, Object>> queuesMap = new HashMap<ObjectName, Map<String,Object>>();
	private Map<ObjectName, Map<String, Object>> topicsMap = new HashMap<ObjectName, Map<String,Object>>();
	
	private Set<String> brokerExcludeMetrics = new HashSet<String>();
	private Set<String> queueExcludeMetrics = new HashSet<String>();
	private Set<String> topicExcludeMetrics = new HashSet<String>();
	
	@Override
	public TaskOutput execute(Map<String, String> taskArguments,
			TaskExecutionContext arg1) throws TaskExecutionException
	{
		try
		{
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
			
			ActiveMQWrapper activeMQWrapper = new ActiveMQWrapper();
			if(host != null && host !="" && port != null && port !="" && userName != null && userName !="" && password != null && password !="")
			{
				activeMQWrapper.connect(host, port, userName, password);
			} else 
			{
				LOG.error("Credentials null or empty in monitor.xml");
				throw new RuntimeException("Credentials null or empty in monitor.xml");
			}
			
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
					printMetric(getMetricPrefix() + broker + "| Queue |" + queueName.getKeyProperty("destinationName") + "|", queMetrics.getKey(), queMetrics.getValue(), MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
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
					printMetric(getMetricPrefix() + broker + "| Topic |" + topicName.getKeyProperty("destinationName") + "|", topMetrics.getKey(), topMetrics.getValue(), MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
				}
			}
			return new TaskOutput("ActiveMQ Metric Upload Complete");
		} catch (Exception e)
		{
			LOG.error("ActiveMQ Metric upload failed");
			return new TaskOutput("ActiveMQ Metric upload failed");
		}
	}
	
	private void printMetric(String metricPath, String metricName, Object metricValue, String aggregation, String timeRollup, String cluster)
    {
        MetricWriter metricWriter = super.getMetricWriter(metricPath + metricName, aggregation,
                timeRollup,
                cluster
        );
        if (metricValue instanceof Double)
        {
            metricWriter.printMetric(String.valueOf(Math.round((Double) metricValue)));
        } else if (metricValue instanceof Float) {
            metricWriter.printMetric(String.valueOf(Math.round((Float) metricValue)));
        } else {
            metricWriter.printMetric(String.valueOf(metricValue));
        }
    }
	
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
			LOG.error("Metrics file not found");
		} catch (ParserConfigurationException e)
		{
			LOG.error("Error in instantiating Document Builder");
		} catch (SAXException e)
		{
			LOG.error("Error in parsing file");
		} catch (IOException e)
		{
			LOG.error("Error");
		} finally
		{
			if(metricsFile != null)
				metricsFile.close();
		}
	}

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
	
	private String getMetricPrefix()
	{
		return metricPathPrefix;
	}
}
