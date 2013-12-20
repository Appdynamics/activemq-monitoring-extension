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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;

public class ActiveMQWrapper
{
	private static final Logger LOG = Logger.getLogger(ActiveMQWrapper.class);
	protected MBeanServerConnection connection = null;
	private JMXConnector jmxConnector = null;
	
	protected void connect (final String host, final String port, final String username, final String password) throws Exception
	{
		JMXServiceURL url = null;
		String serviceUrl = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
		final Map<String, Object> env = new HashMap<String, Object>();
		try
		{
			url = new JMXServiceURL(serviceUrl);
			env.put(JMXConnector.CREDENTIALS, new String[] {username, password});
			jmxConnector = JMXConnectorFactory.connect(url, env);
			connection = jmxConnector.getMBeanServerConnection();
		} catch (MalformedURLException e)
		{
			LOG.error("Error while creating "+serviceUrl+". "+e.getMessage());
		} catch (IOException e)
		{
			LOG.error("Connection failed due to wrong credentials/ JMX is not enabled in the configuration or ActiveMQ is down");
		} catch (Exception e)
		{
			LOG.error("Connection failed due to wrong credentials or JMX is not enabled in the configuration");
		}
	}
	
	protected String getBrokerNames() throws Exception
	{
		Set<ObjectName> beanNames = connection.queryNames(new ObjectName("org.apache.activemq:*"), null);
		Set<String> brokers = new HashSet<String>();
		for(ObjectName mbean : beanNames)
		{
			brokers.add(mbean.getKeyProperty("brokerName"));
		}
		Iterator<String> iterator = brokers.iterator();
		String brokerName = iterator.next();
		return brokerName;
	}
	
	protected Map<String, Object> getBrokerMetrics(String broker) throws Exception	
	{
		Map<String, Object> brokerMetrics =  new HashMap<String, Object>();
		// Get Broker and its metrics
		ObjectName brokerBeanName = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker);
		brokerMetrics = getMetricsFromMBean(brokerBeanName);
		return brokerMetrics;
	}
	
	protected Map<ObjectName, Map<String, Object>> getQueueMetrics(String broker, List<String> excludeList) throws Exception	
	{
		Map<ObjectName, Map<String, Object>> queuesMap = new HashMap<ObjectName, Map<String,Object>>();
		// Get Queues and their metrics
		Set<ObjectName> queueBeans = this.connection.queryNames(new ObjectName("org.apache.activemq:*,brokerName=" + broker + ",destinationType=Queue"), null);
		Map<String, Object> queueMetrics = new HashMap<String, Object>();
		for(ObjectName queueBeanName : queueBeans)
		{
			if(!excludeList.contains(queueBeanName.getKeyProperty("destinationName")))
			{
				queueMetrics = getMetricsFromMBean(queueBeanName);
				queuesMap.put(queueBeanName, queueMetrics);
			}
		}
		return queuesMap;
		
	}
	
	protected Map<ObjectName, Map<String, Object>> getTopicMetrics(String broker, List<String> excludeList) throws Exception	
	{
		Map<ObjectName, Map<String, Object>> topicsMap = new HashMap<ObjectName, Map<String,Object>>();
		
		//Get Topics and their metrics
		Set<ObjectName> topicBeans = this.connection.queryNames(new ObjectName("org.apache.activemq:*,brokerName=" + broker + ",destinationType=Topic"), null);
		Map<String, Object> topicMetrics = new HashMap<String, Object>();
		for(ObjectName topicBeanName : topicBeans)
		{
			if(!excludeList.contains(topicBeanName.getKeyProperty("destinationName")))
			{
				topicMetrics = getMetricsFromMBean(topicBeanName);
				topicsMap.put(topicBeanName, topicMetrics);
			}
		}
		return topicsMap;
	}

	private Map<String, Object> getMetricsFromMBean(ObjectName beanName)
			throws InstanceNotFoundException, IntrospectionException,
			ReflectionException, IOException, MBeanException,
			AttributeNotFoundException
	{
		Map<String, Object> metricsMap = new HashMap<String, Object>();
		MBeanAttributeInfo[] attributes = this.connection.getMBeanInfo(beanName).getAttributes();
		for(MBeanAttributeInfo attr : attributes)
		{
			if(attr.isReadable())
			{
				String attributeName = attr.getName();
				Object value = this.connection.getAttribute(beanName, attributeName);
				if(value != null && Number.class.isAssignableFrom(value.getClass()))
				{
					metricsMap.put(attributeName, value);
				}
			}
		}
		return metricsMap;
	}
}
