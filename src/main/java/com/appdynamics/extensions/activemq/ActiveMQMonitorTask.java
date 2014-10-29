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

import com.appdynamics.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.management.MBeanAttributeInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.activemq.config.MBeanData;
import com.appdynamics.extensions.activemq.config.MBeanKeyPropertyInfo;
import com.appdynamics.extensions.activemq.config.Server;
import com.appdynamics.extensions.jmx.JMXConnectionConfig;
import com.appdynamics.extensions.jmx.JMXConnectionUtil;
import com.appdynamics.extensions.util.MetricUtils;
import com.google.common.base.Strings;

public class ActiveMQMonitorTask implements Callable<ActiveMQMetrics> {

	public static final String METRICS_SEPARATOR = "|";
	private Server server;
	private Map<String, MBeanData> mbeanLookup;
	private JMXConnectionUtil_URL jmxConnector;
	public static final Logger logger = Logger.getLogger("com.singularity.extensions.ActiveMQMonitorTask");

	public ActiveMQMonitorTask(Server server, MBeanData[] mbeansData) {
		this.server = server;
		createMBeansLookup(mbeansData);
	}

	private void createMBeansLookup(MBeanData[] mbeansData) {
		mbeanLookup = new HashMap<String, MBeanData>();
		if (mbeansData != null) {
			for (MBeanData mBeanData : mbeansData) {
				mbeanLookup.put(mBeanData.getDomainName(), mBeanData);
			}
		}
	}

	public ActiveMQMetrics call() throws Exception {
		ActiveMQMetrics activeMQMetrics = new ActiveMQMetrics();
		activeMQMetrics.setDisplayName(server.getDisplayName());
		try {
			jmxConnector = new JMXConnectionUtil_URL(new JMXConnectionConfig_URL(server.getUrl(), server.getUsername(),
					server.getPassword()));
			JMXConnector connector = jmxConnector.connect();
			if (connector != null) {
				Set<ObjectInstance> allMbeans = jmxConnector.getAllMBeans();
				if (allMbeans != null) {
					Map<String, String> filteredMetrics = applyExcludePatternsAndExtractMetrics(allMbeans);
					filteredMetrics.put(ActiveMQMonitorConstants.METRICS_COLLECTION_SUCCESSFUL, ActiveMQMonitorConstants.SUCCESS_VALUE);
					activeMQMetrics.setMetrics(filteredMetrics);
				}
			}
		} catch (Exception e) {
			logger.error("Error JMX-ing into the server :: " + activeMQMetrics.getDisplayName() + e);
			activeMQMetrics.getMetrics().put(ActiveMQMonitorConstants.METRICS_COLLECTION_SUCCESSFUL, ActiveMQMonitorConstants.ERROR_VALUE);
		} finally {
			jmxConnector.close();
		}
		return activeMQMetrics;
	}

	private Map<String, String> applyExcludePatternsAndExtractMetrics(Set<ObjectInstance> allMbeans) throws MalformedObjectNameException, NullPointerException {
		Map<String, String> filteredMetrics = new HashMap<String, String>();
		for (ObjectInstance mbean : allMbeans) {
			ObjectName objectName = mbean.getObjectName();
			if (isDomainConfigured(objectName)) {
				MBeanData mBeanData = mbeanLookup.get(objectName.getDomain());
				Set<String> excludePatterns = mBeanData.getExcludePatterns();
				MBeanAttributeInfo[] attributes = jmxConnector.fetchAllAttributesForMbean(objectName);
				if (attributes != null) {
					for (MBeanAttributeInfo attr : attributes) {
						// See we do not violate the security rules, i.e. only
						// if the attribute is readable.
						if (attr.isReadable()) {
							Object attribute = jmxConnector.getMBeanAttribute(objectName, attr.getName());
							// AppDynamics only considers number values
							if (attribute != null && attribute instanceof Number) {
								String metricKey = getMetricsKey(objectName, attr);
								if (!isKeyExcluded(metricKey, excludePatterns)) {
									if (logger.isDebugEnabled()) {
										logger.debug("Metric key:value before ceiling = " + metricKey + ":" + String.valueOf(attribute));
									}
									String attribStr = MetricUtils.toWholeNumberString(attribute);
									filteredMetrics.put(metricKey, attribStr);
								} else {
									if (logger.isDebugEnabled()) {
										logger.debug(metricKey + " is excluded");
									}
								}
							}
						}
					}
				}
			}
		}
		return filteredMetrics;
	}

	private boolean isKeyExcluded(String metricKey, Set<String> excludePatterns) {
		for (String excludePattern : excludePatterns) {
			if (metricKey.matches(escapeText(excludePattern))) {
				return true;
			}
		}
		return false;
	}

	private String escapeText(String excludePattern) {
		return excludePattern.replaceAll("\\|", "\\\\|");
	}

	private String getMetricsKey(ObjectName objectName, MBeanAttributeInfo attr) {
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
		metricsKey.append(attr.getName());

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

	private boolean isDomainConfigured(ObjectName objectName) {
		return (mbeanLookup.get(objectName.getDomain()) != null);
	}

}
