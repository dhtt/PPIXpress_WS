package com.webserver;

import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final Properties props = new Properties();

    static {
        try (InputStream input =
                 Config.class.getClassLoader()
                     .getResourceAsStream("application.properties")) {

            props.load(input);

        } catch (Exception e) {
            throw new RuntimeException("Cannot load application.properties", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}