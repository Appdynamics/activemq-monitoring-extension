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
