/*
 * Copyright 2013. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.activemq.config;


import com.google.common.collect.Maps;

import java.util.Map;

public class MBean {

    String objectName;
    Map<String,?> metrics;

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public Map<String, ?> getMetrics() {
        if(metrics == null){
            metrics = Maps.newHashMap();
        }
        return metrics;
    }

    public void setMetrics(Map<String, ?> metrics) {
        this.metrics = metrics;
    }
}
