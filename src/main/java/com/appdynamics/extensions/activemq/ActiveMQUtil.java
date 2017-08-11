package com.appdynamics.extensions.activemq;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by bhuvnesh.kumar on 8/9/17.
 */
public class ActiveMQUtil {
    public static String convertToString(final Object field, final String defaultStr) {
        if(field == null) {
            return defaultStr;
        }
        return field.toString();
    }

    public static String[] split(final String metricType,final String splitOn) {
        return metricType.split(splitOn);
    }

    public static String toBigIntString(final BigDecimal bigD) {
        return bigD.setScale(0, RoundingMode.HALF_UP).toBigInteger().toString();
    }

    public static boolean isCompositeObject (String objectName) {
        return (objectName.indexOf('.') != -1);
    }

    public static String getMetricNameFromCompositeObject(String objectName) {
        return objectName.split("\\.")[0];
    }

}
