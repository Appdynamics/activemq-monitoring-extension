package com.appdynamics.extensions.activemq;

public enum ActiveMQMBeansKeyPropertiesEnum {

    NODEID("nodeId"), SERVICE("service"), RESPONSIBILITY("responsibility"), DOMAIN("Domain"), SUBTYPE("subType"),
    CACHE("cache"), TYPE("type"), SCOPE("scope"), NAME("name"), KEYSPACE("keyspace"), PATH("path"), TIER("tier"),
    BROKERNAME("brokerName"),DESTINATIONTYPE("destinationType"), DESTINATIONNAME("destinationName"), DESTINATION("Destination") ;

    private final String name;

    ActiveMQMBeansKeyPropertiesEnum(String name) {
        this.name = name;
    }

    public String toString () {
        return name;
    }
}
