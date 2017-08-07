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
import com.google.common.collect.Lists;

import com.appdynamics.extensions.activemq.config.MBean;
import com.appdynamics.extensions.activemq.config.Server;
import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.openmbean.CompositeDataSupport;
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

	public static final double DEFAULT_MULTIPLIER = 1d;
	public static final String DEFAULT_METRIC_TYPE = "AVERAGE AVERAGE INDIVIDUAL";
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
			if(connector == null){
				throw new IOException("Unable to connect to Mbean server");
			}
			MBeanServerConnection connection = connector.getMBeanServerConnection();
			for(MBean mBean : mbeans){
				try {
					ObjectName objectName = ObjectName.getInstance(mBean.getObjectName());
					Set<ObjectInstance> objectInstances = connection.queryMBeans(objectName, null);
					logger.debug("The ObjectInstances for object name [{}] are {}",mBean.getObjectName(),objectInstances);
					for(ObjectInstance instance : objectInstances){
						//gathering metric names by applying exclude filter if present.
						List excludeMetrics = (List)mBean.getMetrics().get("exclude");
						Set<String> metricsToBeReported = Sets.newHashSet();
						if(excludeMetrics != null){
							gatherMetricNamesByApplyingExcludeFilter(connection, instance, excludeMetrics, metricsToBeReported);
						}
						logger.debug("The metrics after filtering the excludes are {}", metricsToBeReported);
						//gathering metric names by applying include filter if present.
						List includeMetrics = (List)mBean.getMetrics().get("include");
						Map<String,MetricOverride> overrideMap = Maps.newHashMap();
						if(includeMetrics != null){
							gatherMetricNamesByApplyingIncludeFilter(includeMetrics,metricsToBeReported);
							populateOverridesMap(includeMetrics, overrideMap);
						}
						logger.debug("The metrics after filtering the includes are {}", metricsToBeReported);


						/**
						 * Trying to get composite data metrics
						 * The metrics are being stored in metricsToBeReported.
						 *  Split the metric name and then
						 *  Obtain the first part of the attribute and then
						 *  go to the attribute, use the dictionary where you store the sub parts and
						 *  get the required metrics.
						 */


						Map<String,List<String>> compositeAttributesFound = new HashMap<String, List<String>>();
						List<String> attrNames;

						for(String metricName: metricsToBeReported){
							String metricNameFromObj = Util.getMetricNameFromCompositeObject(metricName);
							attrNames  = compositeAttributesFound.get(metricNameFromObj);

							//List<String> attrNames = Lists.newArrayList();

							if (Util.isCompositeObject(metricName)){
								if(attrNames == null){
									attrNames = Lists.newArrayList();
									String metricValueFromObj = Util.getMetricValueFromCompositeObject(metricName);
									attrNames.add(metricValueFromObj);
								}
								else{
									attrNames.add(Util.getMetricValueFromCompositeObject(metricName));
								}

							}
							compositeAttributesFound.put(metricNameFromObj,attrNames);
						}

						// creates a new set of metrics to be reported
						Set<String> newMetricsToBeReported = Sets.newHashSet();
						// adds all metrics that need to be reported except the composite metrics that are found
						for(String metricName: metricsToBeReported){
							if(!Util.isCompositeObject(metricName)){
								newMetricsToBeReported.add(metricName);
							}
						}

						// adds the firstname(AttributeName) of the composite metrics that need to be reported
						for(String metricName: compositeAttributesFound.keySet()){
							newMetricsToBeReported.add(metricName);
						}


						//getting all the metrics from MBean server and overriding them if
//						AttributeList attributeList = connection.getAttributes(instance.getObjectName(), metricsToBeReported.toArray(new String[metricsToBeReported.size()]));


						AttributeList attributeList = connection.getAttributes(instance.getObjectName(), newMetricsToBeReported.toArray(new String[newMetricsToBeReported.size()]));

						List<Attribute> list = attributeList.asList();
						logger.debug("The name-value of the final list of metrics are [{}]", list);
						for (Attribute attr : list) {
							if(isMetricValueValid(attr.getValue())){
								String metricKey = getMetricsKey(instance.getObjectName(),getMetricName(overrideMap,attr.getName()));
								BigInteger bigVal = toBigInteger(attr.getValue(), getMultiplier(overrideMap,attr.getName()));
								String[] metricTypes = getMetricTypes(overrideMap,attr.getName());
								printMetric(formMetricPath(metricKey), bigVal.toString(),metricTypes[0],metricTypes[1],metricTypes[2]);
							}/* checking for composite type metrics */
							else if(isCurrentObjectComposite(attr)){

								Set<String> compositeAttrFound = ((CompositeDataSupport) attr.getValue()).getCompositeType().keySet();
								for(String str : compositeAttrFound){
									String key = attr.getName()+ "." + str;
									if(overrideMap.containsKey(key)){
										String metricKey = getMetricsKey(instance.getObjectName(), getMetricName(overrideMap, key));
										Object compositeAttrValue = ((CompositeDataSupport) attr.getValue()).get(str);
										String[] metricTypes = getMetricTypes(overrideMap,key);

										printMetric(formMetricPath(metricKey), compositeAttrValue.toString(), metricTypes[0],metricTypes[1],metricTypes[2] );
									}
								}
							}
							else{
								logger.debug("The metric value of the attribute [{}]=[{}] is not valid", attr.getName(), attr.getValue());
							}
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
		finally {
			if (connector != null) {
				connector.close();
			}
		}
	}


	private boolean isCurrentObjectComposite (Attribute attribute) {
		return attribute.getValue().getClass().equals(CompositeDataSupport.class);
	}



	private String[] getMetricTypes(Map<String, MetricOverride> overrideMap, String name) {
		if(overrideMap.get(name) == null){
			return DEFAULT_METRIC_TYPE.split(" ");
		}
		MetricOverride override = overrideMap.get(name);
		return new String[]{override.getAggregator(),override.getTimeRollup(),override.getClusterRollup()};
	}

	private Double getMultiplier(Map<String, MetricOverride> overrideMap,String name) {
		if(overrideMap.get(name) == null){
			return DEFAULT_MULTIPLIER;
		}
		return overrideMap.get(name).getMultiplier();
	}

	private String getMetricName(Map<String, MetricOverride> overrideMap, String name) {
		if(overrideMap.get(name) == null){
			return name;
		}
		return overrideMap.get(name).getAlias();
	}

	private JMXConnector createJMXConnector() throws IOException {
		JMXConnector jmxConnector;
		final Map<String, Object> env = new HashMap<String, Object>();
		if(!Strings.isNullOrEmpty(server.getUsername())){
			env.put(JMXConnector.CREDENTIALS,new String[]{server.getUsername(),server.getPassword()});
			logger.debug("Attempting a connection to [{}] with the credentials ****",serviceURL);
			jmxConnector = JMXConnectorFactory.connect(serviceURL, env);
		}
		else{
			logger.debug("Attempting a connection to [{}] without the credentials", serviceURL);
			jmxConnector = JMXConnectorFactory.connect(serviceURL);
		}
		return jmxConnector;
	}


	private void gatherMetricNamesByApplyingExcludeFilter(MBeanServerConnection connection, ObjectInstance instance, List excludeMetrics, Set<String> metrics) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
		MBeanAttributeInfo[] attributes = connection.getMBeanInfo(instance.getObjectName()).getAttributes();
		for(MBeanAttributeInfo attr : attributes){
			if (!excludeMetrics.contains(attr.getName())) {
				if (attr.isReadable()) {
					metrics.add(attr.getName());
				}
			}
		}
	}

	private void gatherMetricNamesByApplyingIncludeFilter(List includeMetrics,Set<String> metrics) {
		for(Object inc : includeMetrics){
			Map metric = (Map) inc;
			//Get the First Entry which is the metric
			Map.Entry firstEntry = (Map.Entry) metric.entrySet().iterator().next();
			String metricName = firstEntry.getKey().toString();
			metrics.add(metricName); //to get jmx metrics
		}
	}


	private String formMetricPath(String metricKey) {
		if(!Strings.isNullOrEmpty(server.getDisplayName())){
			return metricPrefix + server.getDisplayName() + METRICS_SEPARATOR + metricKey;
		}
		return metricPrefix + metricKey;
	}



	private void populateOverridesMap(List includeMetrics, Map<String, MetricOverride> overrideMap) {
		for(Object inc : includeMetrics){
			Map metric = (Map) inc;
			//Get the First Entry which is the metric
			Map.Entry firstEntry = (Map.Entry) metric.entrySet().iterator().next();
			String metricName = firstEntry.getKey().toString();
			MetricOverride override = new MetricOverride();
			override.setAlias(firstEntry.getValue().toString());
			override.setMultiplier(metric.get("multiplier") != null ? Double.parseDouble(metric.get("multiplier").toString()) : DEFAULT_MULTIPLIER);
			String metricType = metric.get("metricType") != null ? metric.get("metricType").toString() : DEFAULT_METRIC_TYPE;
			String[] metricTypes = metricType.split(" ");
			override.setAggregator(metricTypes[0]);
			override.setTimeRollup(metricTypes[1]);
			override.setClusterRollup(metricTypes[2]);
			overrideMap.put(metricName,override);
		}
	}



	private BigInteger toBigInteger(Object value,Double multiplier) {
		try {
			BigDecimal bigD = new BigDecimal(value.toString());
			if(multiplier != null && multiplier != DEFAULT_MULTIPLIER) {
				bigD = bigD.multiply(new BigDecimal(multiplier));
			}
			return bigD.setScale(0, RoundingMode.HALF_UP).toBigInteger();
		}
		catch(NumberFormatException nfe){
		}
		return BigInteger.ZERO;
	}



	private void printMetric(String metricPath,String metricValue,String aggType,String timeRollupType,String clusterRollupType) {
		MetricWriter writer = metricWriter.getMetricWriter(metricPath,
				aggType,
				timeRollupType,
				clusterRollupType
		);
		//System.out.println("Sending [" + aggType + METRICS_SEPARATOR + timeRollupType + METRICS_SEPARATOR + clusterRollupType
		//		+ "] metric = " + metricPath + " = " + metricValue);
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
