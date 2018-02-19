/*
 * Copyright 2013. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.activemq.metrics;


import com.appdynamics.extensions.activemq.ActiveMQMBeansKeyPropertiesEnum;
import com.google.common.base.Strings;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

class MetricKeyFormatter {

    private ObjectName getObjectName (ObjectInstance instance) {
        return instance.getObjectName();
    }

    String getInstanceKey (ObjectInstance instance) {
        if (instance == null) {
            return "";
        }
        // Standard jmx keys. {type, scope, name, keyspace, path etc.}
        String type = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.TYPE.toString());
        String domain = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.DOMAIN.toString());
        String subType = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.SUBTYPE.toString());
        String name = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.NAME.toString());
        String service = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.SERVICE.toString());
        String scope = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.SCOPE.toString());
        String cache = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.CACHE.toString());
        String path = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.PATH.toString());
        String keyspace = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.KEYSPACE.toString());

        StringBuilder metricsKey = new StringBuilder();
        metricsKey.append(Strings.isNullOrEmpty(type) ? "" : type + "|");
        metricsKey.append(Strings.isNullOrEmpty(domain) ? "" : domain + "|");
        metricsKey.append(Strings.isNullOrEmpty(subType) ? "" : subType + "|");
        metricsKey.append(Strings.isNullOrEmpty(service) ? "" : service + "|");
        metricsKey.append(Strings.isNullOrEmpty(path) ? "" : path + "|");
        metricsKey.append(Strings.isNullOrEmpty(scope) ? "" : scope + "|");
        metricsKey.append(Strings.isNullOrEmpty(keyspace) ? "" : keyspace + "|");
        metricsKey.append(Strings.isNullOrEmpty(name) ? "" : name + "|");
        metricsKey.append(Strings.isNullOrEmpty(cache) ? "" : cache + "|");

        return metricsKey.toString();
    }

    private String getKeyProperty (ObjectInstance instance, String property) {
        if (instance == null) {
            return "";
        }
        return getObjectName(instance).getKeyProperty(property);
    }

    String getNodeKey (ObjectInstance instance, String metricName, String instanceKey) {
        StringBuilder metricKey = new StringBuilder(instanceKey);
        String tier = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.TIER.toString());
        String responsibility = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.RESPONSIBILITY.toString());
        metricKey.append(Strings.isNullOrEmpty(tier) ? "" : tier + "|");
        metricKey.append(Strings.isNullOrEmpty(responsibility) ? "" : responsibility + "|");
        metricKey.append(metricName);
        return metricKey.toString();
    }
}
