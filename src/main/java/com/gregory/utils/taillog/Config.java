package com.gregory.utils.taillog;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Gera
 * Date: 15.02.14
 * Time: 1:51
 * To change this template use File | Settings | File Templates.
 */
//TODO: make external loader instead of creating temp properties
public class Config {
    private Properties temp;

    public static Config load() {
       Config config = new Config();
        Properties prop = new Properties();
        prop.setProperty("host", "192.168.1.5");
        prop.setProperty("log", "/home/gregory/hello.txt");
        prop.setProperty("user", "gregory");
        prop.setProperty("passwd", "gregory");

        config.setTemp(prop);
        return config;
    }

    public Properties getTargetConfig(String host, String log) {
       return temp;
    }


    public List<String> getLogFiles() {
        ArrayList<String> list = new ArrayList<String>();
        list.add(temp.getProperty("log"));
        return list;
    }

    private void setTemp(Properties temp) {
        this.temp = temp;
    }
}
