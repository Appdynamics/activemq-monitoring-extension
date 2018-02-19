/*
 * Copyright 2013. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.activemq;


import com.appdynamics.extensions.activemq.metrics.*;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.activemq.Constants.DISPLAY_NAME;


public class ActiveMQMonitorTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQMonitorTask.class);
    private static final BigDecimal ERROR_VALUE = BigDecimal.ZERO;
    private static final BigDecimal SUCCESS_VALUE = BigDecimal.ONE;
    private static final String METRICS_COLLECTION_SUCCESSFUL = "Metrics Collection Successful";
    private String metricPrefix;
    private MetricWriteHelper metricWriter;
    private Map server;
    private JMXConnectionAdapter jmxConnectionAdapter;
    private List<Map> configMBeans;
    private String serverName;

    public void run() {
        serverName = ActiveMQUtil.convertToString(server.get(DISPLAY_NAME), "");
        MetricPrinter metricPrinter = new MetricPrinter(metricPrefix, serverName, metricWriter);

        try {
            logger.debug("ActiveMQ monitoring task initiated for server {}", serverName);
            BigDecimal status = populateAndPrintStats(metricPrinter);

            metricPrinter.printMetric(metricPrinter.formMetricPath(METRICS_COLLECTION_SUCCESSFUL), status,
                    MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
        } catch (Exception e) {
            logger.error("Error in ActiveMQ Monitoring Task for Server {}", serverName, e);
            metricPrinter.printMetric(metricPrinter.formMetricPath(METRICS_COLLECTION_SUCCESSFUL), BigDecimal.ZERO,
                    MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
        } finally {
            logger.debug("ActiveMQ Monitoring Task Complete. Total number of metrics reported = {}", metricPrinter
                    .getTotalMetricsReported());
        }
    }

    private BigDecimal populateAndPrintStats(MetricPrinter metricPrinter) throws IOException {
        JMXConnector jmxConnector = null;

        try {
            jmxConnector = jmxConnectionAdapter.open();
            logger.debug("JMX Connection is now open");
            MetricPropertiesBuilder propertiesBuilder = new MetricPropertiesBuilder();

            for (Map mBean : configMBeans) {
                String configObjName = ActiveMQUtil.convertToString(mBean.get("objectName"), "");
                logger.debug("Processing mBeam {} from the config file", configObjName);

                try {
                    Map<String, MetricProperties> metricProperties = propertiesBuilder.build(mBean);
                    NodeMetricsProcessor nodeMetricsProcessor = new NodeMetricsProcessor(jmxConnectionAdapter, jmxConnector);
                    List<Metric> nodeMetrics = nodeMetricsProcessor.getNodeMetrics(mBean, metricProperties, metricPrefix);

                    if (nodeMetrics.size() > 0) {
                        metricPrinter.reportNodeMetrics(nodeMetrics);
                    }
                } catch (MalformedObjectNameException e) {
                    logger.error("Illegal Object Name {} " + configObjName, e);
                } catch (Exception e) {
                    logger.error("Error fetching JMX metrics for {} and mBeam = {}", serverName, configObjName, e);
                }
            }
        } finally {
            try {
                jmxConnectionAdapter.close(jmxConnector);
                logger.debug("JMX connection is closed.");
            } catch (IOException e) {
                logger.error("Unable to close the JMX connection.");
                return ERROR_VALUE;
            }
        }
        return SUCCESS_VALUE;
    }


    public static class Builder {
        private ActiveMQMonitorTask task = new ActiveMQMonitorTask();

        Builder metricPrefix(String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        Builder metricWriter(MetricWriteHelper metricWriter) {
            task.metricWriter = metricWriter;
            return this;
        }

        Builder server(Map server) {
            task.server = server;
            return this;
        }

        Builder jmxConnectionAdapter(JMXConnectionAdapter adapter) {
            task.jmxConnectionAdapter = adapter;
            return this;
        }

        Builder mbeans(List<Map> mBeans) {
            task.configMBeans = mBeans;
            return this;
        }

        ActiveMQMonitorTask build() {
            return task;
        }
    }
}
