/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */
package com.appdynamics.extensions.activemq.config;

import com.google.common.collect.Lists;

import java.util.List;


public class Configuration {

	private static final int DEFAULT_NUMBER_OF_THREADS = 10;

	private String metricPrefix;
	private List<Server> servers;
    private int numberOfThreads = DEFAULT_NUMBER_OF_THREADS;
	private String encryptionKey;
	private List<MBean> mbeans;

	public List<Server> getServers() {
		if(servers == null){
			servers = Lists.newArrayList();
		}
		return servers;
	}

	public void setServers(List<Server> servers) {
		this.servers = servers;
	}

	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public String getMetricPrefix() {
		return metricPrefix;
	}

	public void setMetricPrefix(String metricPrefix) {
		if(!metricPrefix.endsWith("|")){
			metricPrefix = metricPrefix + "|";
		}
		this.metricPrefix = metricPrefix;
	}

	public String getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public List<MBean> getMbeans() {
		if(mbeans == null){
			mbeans = Lists.newArrayList();
		}
		return mbeans;
	}

	public void setMbeans(List<MBean> mbeans) {
		this.mbeans = mbeans;
	}
}
