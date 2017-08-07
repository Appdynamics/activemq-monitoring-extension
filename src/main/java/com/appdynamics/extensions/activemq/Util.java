package com.appdynamics.extensions.activemq;

/**
 * Created by bhuvnesh.kumar on 8/1/17.
 */
public class Util {



    public static String convertToString(final Object field, final String defaultStr) {
        if(field == null) {
            return defaultStr;
        }
        return field.toString();
    }

    public static String[] split(final String metricType,final String splitOn) {
        return metricType.split(splitOn);
    }


    public static boolean isCompositeObject (String objectName) {
        return (objectName.indexOf('.') != -1);
    }

    public static String getMetricNameFromCompositeObject(String objectName) {
        return objectName.split("\\.")[0];
    }

    public static String getMetricValueFromCompositeObject(String objectName) {
        return objectName.split("\\.")[1];
    }

}
