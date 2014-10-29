/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.appdynamics.util;

/**
 *
 * @author gilbert.solorzano
 */
public class JMXConnectionConfig_URL {
    private String host;
    private int port;
    private String url;
    private String username;
    private String password;

    public JMXConnectionConfig_URL(String url, String username, String password) {
        this.url=url;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
