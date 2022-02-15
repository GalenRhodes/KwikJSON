package com.galenrhodes.kwikjson;

import java.io.InputStream;
import java.util.Properties;

public class KwikProperties extends Properties {
    public KwikProperties(String filename) {
        try(InputStream inputStream = getClass().getResourceAsStream(filename)) { load(inputStream); }
        catch(Exception e) { throw new RuntimeException(e); }
    }

    public char getChar(String key, char defaultValue) {
        String str = getProperty(key);
        return (((str == null) || (str.length() < 1)) ? defaultValue : str.charAt(0));
    }

    public char getChar(String key) { return getChar(key, (char)0); }

    public int getInteger(String key, int defaultValue) {
        try {
            return Integer.parseInt(getProperty(key, String.valueOf(defaultValue)));
        }
        catch(Exception e) {
            return defaultValue;
        }
    }

    public int getInteger(String key) { return getInteger(key, 0); }
}
