/**
 * Copyright 2014 AppDynamics, Inc.
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

import com.appdynamics.extensions.activemq.config.MBean;
import com.appdynamics.extensions.activemq.config.Server;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.appdynamics.extensions.activemq.ActiveMQMonitorConstants.*;
import static com.appdynamics.extensions.util.metrics.MetricConstants.METRICS_SEPARATOR;

public class ActiveMQMonitorTask implements Runnable {

	private String metricPrefix;
	private Server server;
	private AManagedMonitor metricWriter;
	private JMXServiceURL serviceURL;
	private List<MBean> mbeans;
	public static final Logger logger = LoggerFactory.getLogger(ActiveMQMonitorTask.class);

	public void run() {
		long startTime = System.currentTimeMillis();
		try {
			logger.debug("ActiveMQ monitor thread for server {} started.",server.getDisplayName());
			extractAndReportMetrics();
			printMetric(formMetricPath(METRICS_COLLECTION_SUCCESSFUL), SUCCESS_VALUE
					, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
		} catch (Exception e) {
			logger.error("Error in Active MQ Monitor thread for server {}", server.getDisplayName(), e);
			printMetric(formMetricPath(METRICS_COLLECTION_SUCCESSFUL), ERROR_VALUE
					, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

		}
		finally{
			long endTime = System.currentTimeMillis() - startTime;
			logger.debug("ActiveMQ monitor thread for server {} ended. Time taken = {}",server.getDisplayName(),endTime);
		}
	}

	private void extractAndReportMetrics() throws IOException {
		JMXConnector connector = null;
		try{
			connector = createJMXConnector();
			MBeanServerConnection connection = connector.getMBeanServerConnection();
			for(MBean mBean : mbeans){
				try {
					ObjectName objectName = ObjectName.getInstance(mBean.getObjectName());
					Set<ObjectInstance> objectInstances = connection.queryMBeans(objectName, null);
					for(ObjectInstance instance : objectInstances){
						List includeMetrics = (List)mBean.getMetrics().get("include");
						List excludeMetrics = (List)mBean.getMetrics().get("exclude");
						//include and exclude may co-exist if some metrics need to be excluded
						//and some metrics need to be overridden.
						if(includeMetrics != null && excludeMetrics != null){
							reportIncludeExcludeMetrics(connection, instance, includeMetrics,excludeMetrics);
						}
						else if(includeMetrics != null){
							reportIncludeMetrics(connection,instance,includeMetrics);
						}
						else{
							reportExcludeMetrics(connection,instance,excludeMetrics);
						}
					}
				}
				catch(MalformedObjectNameException e){
					logger.error("Illegal object name {}" + mBean.getObjectName(),e);
				}
				catch (Exception e){
					logger.error("Error fetching JMX metrics for {} and mbean={}", server.getDisplayName(),mBean.getObjectName(),e);
				}
			}
		}
		finally{
			connector.close();
		}
	}

	private JMXConnector createJMXConnector() throws IOException {
		JMXConnector jmxConnector;
		final Map<String, Object> env = new HashMap<String, Object>();
		if(!Strings.isNullOrEmpty(server.getUsername())){
			env.put(JMXConnector.CREDENTIALS,new String[]{server.getUsername(),server.getPassword()});
			jmxConnector = JMXConnectorFactory.connect(serviceURL, env);
		}
		else{
			jmxConnector = JMXConnectorFactory.connect(serviceURL);
		}
		return jmxConnector;
	}


	private void reportIncludeExcludeMetrics(MBeanServerConnection connection, ObjectInstance instance, List includeMetrics, List excludeMetrics) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
		Map<String,Double> multiplierMap = Maps.newHashMap();
		Map<String,String> aliasMap = Maps.newHashMap();
		Map<String,String> metricTypeMap = Maps.newHashMap();
		namesToPropertiesMap(includeMetrics,aliasMap,multiplierMap,metricTypeMap);

		List<String> metrics = filterByExclude(connection, instance, excludeMetrics);
		AttributeList attributeList = connection.getAttributes(instance.getObjectName(), metrics.toArray(new String[metrics.size()]));
		List<Attribute> list = attributeList.asList();
		for (Attribute attr : list) {
			if(isMetricValueValid(attr.getValue())){
				String metricKey = getMetricsKey(instance.getObjectName(),aliasMap.get(attr.getName()) == null ? attr.getName() : aliasMap.get(attr.getName()));
				BigInteger bigVal = toBigInteger(attr.getValue(), multiplierMap.get(attr.getName()));
				if(metricTypeMap.get(attr.getName()) == null){
					printAverageAverageIndividual(formMetricPath(metricKey),bigVal);
				}
				else {
					String[] metricTypes = metricTypeMap.get(attr.getName()).split(" ");
					printMetric(formMetricPath(metricKey), bigVal.toString(),metricTypes[0],metricTypes[1],metricTypes[2]);
				}
			}
		}
	}

	private String formMetricPath(String metricKey) {
		return metricPrefix + server.getDisplayName() + METRICS_SEPARATOR + metricKey;
	}

	private List<String> filterByExclude(MBeanServerConnection connection, ObjectInstance instance, List excludeMetrics) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
		MBeanAttributeInfo[] attributes = connection.getMBeanInfo(instance.getObjectName()).getAttributes();
		List<String> metrics = Lists.newArrayList();
		for(MBeanAttributeInfo attr : attributes){
			if (!excludeMetrics.contains(attr.getName())) {
				if (attr.isReadable()) {
					metrics.add(attr.getName());
				}
			}
		}
		return metrics;
	}

	private void reportIncludeMetrics(MBeanServerConnection connection, ObjectInstance instance, List includeMetrics) throws InstanceNotFoundException, IOException, ReflectionException {
		//first get all metric names
		List<String> metrics = getMetricNames(includeMetrics);
		Map<String,Double> multiplierMap = Maps.newHashMap();
		Map<String,String> aliasMap = Maps.newHashMap();
		Map<String,String> metricTypeMap = Maps.newHashMap();
		namesToPropertiesMap(includeMetrics,aliasMap,multiplierMap,metricTypeMap);

		AttributeList attributeList = connection.getAttributes(instance.getObjectName(), metrics.toArray(new String[metrics.size()]));
		List<Attribute> list = attributeList.asList();
		for (Attribute attr : list) {
			if(isMetricValueValid(attr.getValue())){
				String metricKey = getMetricsKey(instance.getObjectName(),aliasMap.get(attr.getName()));
				BigInteger bigVal = toBigInteger(attr.getValue(), multiplierMap.get(attr.getName()));
				if(metricTypeMap.get(attr.getName()) == null){
					printAverageAverageIndividual(formMetricPath(metricKey),bigVal);
				}
				else {
					String[] metricTypes = metricTypeMap.get(attr.getName()).split(" ");
					printMetric(formMetricPath(metricKey), bigVal.toString(),metricTypes[0],metricTypes[1],metricTypes[2]);
				}
			}
		}
	}

	private void namesToPropertiesMap(List includeMetrics, Map<String, String> aliasMap, Map<String, Double> multiplierMap, Map<String, String> metricTypeMap) {
		for(Object inc : includeMetrics){
			Map metric = (Map) inc;
			//Get the First Entry which is the metric
			Map.Entry firstEntry = (Map.Entry) metric.entrySet().iterator().next();
			String metricName = firstEntry.getKey().toString();
			aliasMap.put(metricName,firstEntry.getValue().toString());
			multiplierMap.put(metricName,(metric.get("multiplier") != null) ? Double.parseDouble(metric.get("multiplier").toString()) : 1d);
			metricTypeMap.put(metricName,(metric.get("metricType") != null) ? metric.get("metricType").toString() : null);
		}
	}

	private List<String> getMetricNames(List includeMetrics) {
		List<String> metrics = Lists.newArrayList();
		for(Object inc : includeMetrics){
			Map metric = (Map) inc;
			//Get the First Entry which is the metric
			Map.Entry firstEntry = (Map.Entry) metric.entrySet().iterator().next();
			String metricName = firstEntry.getKey().toString();
			metrics.add(metricName); //to get jmx metrics
		}
		return metrics;
	}


	private void reportExcludeMetrics(MBeanServerConnection connection,ObjectInstance instance,List excludeMetrics) throws Exception {
		List<String> metrics = filterByExclude(connection, instance, excludeMetrics);
		AttributeList attributeList = connection.getAttributes(instance.getObjectName(), metrics.toArray(new String[metrics.size()]));
		List<Attribute> list = attributeList.asList();
		for (Attribute attr : list) {
			if(isMetricValueValid(attr.getValue())){
				String metricKey = getMetricsKey(instance.getObjectName(),attr.getName());
				printAverageAverageIndividual(formMetricPath(metricKey),toBigInteger(attr.getValue(),1d));
			}
		}
	}

	private BigInteger toBigInteger(Object value,Double multiplier) {
		try {
			BigDecimal bigD = new BigDecimal(value.toString());
			if(multiplier != null && multiplier != 1d) {
				bigD = bigD.multiply(new BigDecimal(multiplier));
			}
			return bigD.setScale(0, RoundingMode.HALF_UP).toBigInteger();
		}
		catch(NumberFormatException nfe){
		}
		return BigInteger.ZERO;
	}


	private void printAverageAverageIndividual(String metricPath,BigInteger metricValue){
		printMetric(metricPath,metricValue.toString(),MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
	}


	private void printMetric(String metricPath,String metricValue,String aggType,String timeRollupType,String clusterRollupType) {
		MetricWriter writer = metricWriter.getMetricWriter(metricPath,
				aggType,
				timeRollupType,
				clusterRollupType
		);
		System.out.println("Sending [" + aggType + METRICS_SEPARATOR + timeRollupType + METRICS_SEPARATOR + clusterRollupType
				+ "] metric = " + metricPath + " = " + metricValue);
		logger.debug("Sending [{}|{}|{}] metric= {},value={}", aggType, timeRollupType, clusterRollupType,metricPath,metricValue);
		writer.printMetric(metricValue);
	}



	private String getMetricsKey(ObjectName objectName, String metricName) {
		// Standard jmx keys. {type, brokerName, destinationType, destinationName}
		MBeanKeyPropertyInfo mbeanInfo = determineMBeanKeyProperties(objectName);
		String type = mbeanInfo.getBrokerType();
		String brokerName = mbeanInfo.getBrokerName();
		String destinationType = mbeanInfo.getType();
		String destinationName = mbeanInfo.getDestinationName();
		StringBuilder metricsKey = new StringBuilder();
		metricsKey.append(Strings.isNullOrEmpty(type) ? "" : type + METRICS_SEPARATOR);
		metricsKey.append(Strings.isNullOrEmpty(brokerName) ? "" : brokerName + METRICS_SEPARATOR);
		metricsKey.append(Strings.isNullOrEmpty(destinationType) ? "" : destinationType + METRICS_SEPARATOR);
		metricsKey.append(Strings.isNullOrEmpty(destinationName) ? "" : destinationName + METRICS_SEPARATOR);
		metricsKey.append(metricName);
		return metricsKey.toString();
	}

	private MBeanKeyPropertyInfo determineMBeanKeyProperties(ObjectName objectName) {
		MBeanKeyPropertyInfo mbeanInfo = new MBeanKeyPropertyInfo();
		if (objectName.getKeyProperty("type") == null) {
			mbeanInfo.setBrokerName(objectName.getKeyProperty("BrokerName"));
			mbeanInfo.setType(objectName.getKeyProperty("Type"));
			mbeanInfo.setDestinationName(objectName.getKeyProperty("Destination"));
		} else {
			mbeanInfo.setBrokerType(objectName.getKeyProperty("type"));
			mbeanInfo.setBrokerName(objectName.getKeyProperty("brokerName"));
			mbeanInfo.setType(objectName.getKeyProperty("destinationType"));
			mbeanInfo.setDestinationName(objectName.getKeyProperty("destinationName"));
		}
		return mbeanInfo;
	}

	private boolean isMetricValueValid(Object metricValue) {
		if(metricValue == null){
			return false;
		}
		if(metricValue instanceof String){
			try {
				Double.valueOf((String) metricValue);
				return true;
			}
			catch(NumberFormatException nfe){
			}
		}
		else if(metricValue instanceof Number){
			return true;
		}
		return false;
	}


	public static class Builder {
		private ActiveMQMonitorTask task = new ActiveMQMonitorTask();

		public Builder metricPrefix(String metricPrefix) {
			task.metricPrefix = metricPrefix;
			return this;
		}

		public Builder metricWriter(ActiveMQMonitor metricWriter) {
			task.metricWriter = metricWriter;
			return this;
		}

		public Builder server(Server server){
			task.server = server;
			return this;
		}

		public Builder serviceURL(JMXServiceURL serviceURL){
			task.serviceURL = serviceURL;
			return this;
		}

		public Builder mbeans(List<MBean> mBeans){
			task.mbeans = mBeans;
			return this;
		}

		public ActiveMQMonitorTask build() {
			return task;
		}
	}

}
