# AppDynamics ActiveMQ Monitoring Extension

## Use Case

ActiveMQ is an open source, JMS 1.1 compliant, message-oriented middleware (MOM) from the Apache Software Foundation that provides high-availability, performance, scalability, reliability and security for enterprise messaging. 
The ActiveMQ Monitoring extension collects metrics from an ActiveMQ messaging server and uploads them to the AppDynamics Metric Browser. 

## Prerequisites 

1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
2. JMX must be enabled in your JMX based server for this extension to gather metrics. Please make sure you have all the permissions before deploying the extension.  To enable, please see [these instructions](http://activemq.apache.org/jmx.html)
3. In order to use this extension, you do need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/PRO44/Java+Agent) or [SIM Agent](https://docs.appdynamics.com/display/PRO44/Server+Visibility). 
For more details on downloading these products, please  visit [download.appdynamics.com](https://download.appdynamics.com/).


## Installation

1. To build from source, clone this repository and run 'mvn clean install'. This will produce a JMXMonitor-VERSION.zip in the target directory. 
Alternatively, download the latest release archive from [Github](https://github.com/Appdynamics/activemq-monitoring-extension/releases).
2. Unzip the file ActiveMQ-[version].zip into `<MACHINE_AGENT_HOME>/monitors/`.
3. In the newly created directory "ActiveMQ", edit the config.yml configuring the parameters (See Configuration section below).
4. Make sure you place it in the right directory on your computer. 
5. Restart the Machine Agent
6. In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | ActiveMQ Monitor  .
7. If you're monitoring multiple ActiveMQ instances, follow the above steps for every ActiveMQ instance that you want to monitor.

**NOTE:** Please place the extension in the **"monitors"** directory of your Machine Agent installation
 directory. Do not place the extension in the **"extensions"** directory of your Machine Agent 
 installation directory.


## Configuration ##

Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

Before configuring the extension, please make sure to run the below steps to check if the set up is correct.

1. Telnet into your activemq server from the box where the extension is deployed.
       telnet <hostname> <port>

       <port> - It is the jmxremote.port specified.
        <hostname> - IP address

    If telnet works, it confirm the access to the activemq server.


2. Start jconsole. Jconsole comes as a utitlity with installed jdk. After giving the correct host and port , check if activemq
mbean shows up.


3. Configure the ActiveMQ instances by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ActiveMQMonitor/`.
4. There are a few fields that you need to make sure are filled in correctly. 
Once done with them, they should allow you to establish a successful connection
 with your server. They are : 
```
servers:
  -   displayName: ""
      host: ""
      port:
#      serviceUrl: ""
      username: ""
      password: ""
      #encryptedPassword: ""
```
* displayName: This will be the name of your server that you would like to see on the metric browser.
* host: This is the HostURL that is used with a port to create a connection with the ActiveMQ Server.
* serviceUrl: This is the full URL with host and port that is used to establish a connection. 

**You should either use HOST AND PORT or just the SERVICEURL in order to establish a connection.**

* username: List the username, if any, that is needed to establish a connection.
* password: List the password associated with the username that is needed to establish a connection.
* encryptedPassword: In case you would like to use an encrypted password, use this field.

5. Configure the encyptionKey for encryptionPasswords(only if password encryption required).
    For example,
 ```
    #Encryption key for Encrypted password.
    encryptionKey: "axcdde43535hdhdgfiniyy576"
 ```
6. Configure the numberOfThreads
    For example,
    If number of servers that need to be monitored is 3, then number of threads required is 5 * 3 = 15
 ```
    numberOfThreads: 15
 ```  

* encryptionKey: If you use an encryptedPassword, please provide the key here as well in order for the system to decrypt your password.

**You should either use the Normal PASSWORD or the encryptedPassword and encryptionKey in order to establish a connection. Please read below to find more information on Password Encryption.**

7. 
The metricPrefix of the extension has to be configured as [specified here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695#Configuring%20an%20Extension). Please make sure that the right metricPrefix is chosen based on your machine agent deployment, otherwise this could lead to metrics not being visible in the controller.
Configure the "tier" under which the metrics need to be reported. This can be done by changing the value of `<TIER NAME OR TIER ID>` in
     metricPrefix: "Server|Component:`<TIER NAME OR TIER ID>`|Custom Metrics|ActiveMQ". For example,
    
```
     metricPrefix: "Server|Component:Extensions tier|Custom Metrics|ActiveMQ"
```

The objectNames mentioned in the above yaml may not match your environment exactly. Please use jconsole to extract the objectName and configure it
accordingly in the config.yaml.

## Metrics

You can use this extension to get all metrics that are available through the JMX Messaging service. In order to do so though, you will have to make sure that all metrics are defined correctly.
Please follow the next few steps in order to get this right.
1. You will have to list each mBean separately in the config.yml file. 
For each mBean you will have to add an **objectName**, **mbeanKeys** and **metrics** tag.
The following example shows exactly how you should do that. 
* You will have to each and every **mBeanKey** that is listed in the **objectName**.
* Under **metrics** is where you have the ability to include all the metrics that you would like to monitor.
```
mbeans:
  - objectName: "org.apache.activemq:type=Broker,brokerName=*"
    mbeanKeys: ["type", "brokerName"]
    metrics:
      include:
        - name: "StorePercentUsage"
          alias: "Store Percent Usage"

```
2. There are several properties that are associated with each metric. They are: 
    * alias
    * aggregationType
    * timeRollUpType
    * clusterRollUpType
    * multiplier
    * convert
    * delta
   
   This format enables you to change some of the metric properties from what the default configurations are.

    In Order to use them for each metric, please use the following example.
```
  - objectName: "org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=*"
    mbeanKeys: ["type", "brokerName","destinationType","destinationName"]
    metrics:
      include:
        - name: "AverageEnqueueTime"
          alias: "Average Enqueue Time"
          clusterRollUpType: "AVERAGE"
          timeRollUpType: "SUM"
          aggregationType: "SUM"
```

3. This extension can  be used to get values from **composite objects**. 
In order to do so, you have to list the metric name as is and then specify the path with a **"|"** followed my the composite attribute.
In this example we see that HeapMemoryUsage is a composite object that has 4 values associated with it. 
Now in order to monitor them, you list the property and then in the alias name, add the **"|"** followed by the attribute name in order to get all of the attributes associated with HeapMemoryUsage under one folder in the metric browser.

```
  - objectName: "java.lang:type=Memory"
    mbeanKeys: ["type"]
    metrics:
      include:
        - name: "HeapMemoryUsage.committed"
          alias: "Heap Memory Usage|Committed"
        - name: "HeapMemoryUsage.used"
          alias: "Heap Memory Usage|Used"
```

4. This extension can be used to get values from Map Objects as well. 
To do so, list the metrics you would like to retrieve from the map in the following manner. 
 ```
  - objectName: "java.lang:type=Memory"
    mbeanKeys: ["type"]
    metrics:
      include:
         # Map Metric Level 1
         - name: "MapOfString.key1"
           alias: "Map 1|Key 1"
         - name: "MapOfString.key2"
           alias: "Map 1|Key 2"
 
         # Map Metric Level 2
         - name: "MapOfString.map2.key2"
           alias: "Map 1|Map 2|Key 2"
 
         # Map Metric Level 3
         - name: "MapOfString.map2.map3.key32"
           alias: "Map 1|Map 2|Map 3|Key 32"
           multiplier: "20"
           delta: false
           aggregationType: "OBSERVATION"
           timeRollUpType: "AVERAGE"
           clusterRollUpType: "INDIVIDUAL"
           convert : {
             "ENDANGERED" : "1",
             "NODE-SAFE" : "2",
             "MACHINE-SAFE" : "3"
           }
```
    
5. This extension can be used to get data from List Objects as well. 
    To do so, the metric in the list should be separated with a separator such as a ":" and should be in a key value pair form.
    If your metric is not in the form listed about, the extension will not be able to extract that data. 

``` 
 - objectName: "java.lang:type=Memory"
    mbeanKeys: ["type"]
    metrics:
      include:
        # List Metrics Can be set in the following ways:
        - name: "listOfString.metric one"
          alias: "listOfString|metric one"
        - name: "listOfString.metric two"
          alias: "listOfString|metric two"
        - name: "listOfString.metric three" 
          alias: "listOfString|metric three"
```    



## metricPathReplacements ##

Please visit [this](https://community.appdynamics.com/t5/Knowledge-Base/Metric-Path-CharSequence-Replacements-in-Extensions/ta-p/35412) page to get detailed instructions on configuring Metric Path Character sequence replacements in Extensions.

## Credentials Encryption ##

Please visit [Encryption Guidelines] to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.
If you want to use password encryption, please send arguments as connectionProperties. You will have to fill in the encrypted Password and Encryption Key fields in the config but you will also have to give an empty "" value to the password field and the encrypted password will be automatically picked up.

## Extensions Workbench ##

Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually
 deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench ]

## Troubleshooting ##

Please follow the steps listed in this [troubleshooting-document] in order to troubleshoot your issue.
These are a set of common issues that customers might have faced during the installation of the extension.

## Contributing ##

Always feel free to fork and contribute any changes directly here on [GitHub].

## Version ##

|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |5.1.1       |
|Controller Compatibility  |3.7 or Later|
|Product Tested On         |ActiveMQ 5.16.0 |
|Last Update               |12/01/2021 |
|List of Changes           |[Change log](https://github.com/Appdynamics/activemq-monitoring-extension/blob/master/Changelog.md) |


**List of Changes can be found in the [Changelog.md]**


[How to use the Extensions WorkBench ]: https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130
[Changelog.md]: https://github.com/Appdynamics/activemq-monitoring-extension/blob/1.0.0/Changelog.md
[Encryption Guidelines]: https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397
[troubleshooting-document]: https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695
[AppDynamics Exchange]: https://www.appdynamics.com/community/exchange/extension/activemq-monitoring-extension/
[GitHub]: https://github.com/Appdynamics/activemq-monitoring-extension/
