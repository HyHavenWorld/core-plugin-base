package com.hyhavenworld.core.util;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public class YamlUtil {


    private final Map<String, Object> data;

    public YamlUtil(File file) throws IOException {
        Yaml yaml = new Yaml();
        this.data = yaml.load(new FileInputStream(file));
    }

    /**
     * Returns the value in the yml giving the chain in dot notation or throws exception if no key is found
     * @param path key dot split
     * @return the found value
     * @throws YamlException if any key is not found
     */
    public Object getRequired(String path) {
        String[] keys = path.split("\\.");
        Object current = data;

        for (String key : keys) {
            if (!(current instanceof Map)) {
                throw new YamlException(
                        "The key '" + key + "' is not a map in the route '" + path + "'"
                );
            }

            Map<?, ?> map = (Map<?, ?>) current;
            if (!map.containsKey(key)) {
                throw new YamlException(
                        "The key '" + key + "' is missing in the route '" + path + "'"
                );
            }

            current = map.get(key);
        }

        return current;
    }

    public String getString(String path) {
        Object value = getRequired(path);
        return value.toString();
    }

    public boolean getBoolean(String path) {
        Object value = getRequired(path);
        if (value instanceof Boolean) return (Boolean) value;
        throw new YamlException("Value of '" + path + "' is not a boolean");
    }

    public int getInt(String path) {
        Object value = getRequired(path);
        if (value instanceof Number) return ((Number) value).intValue();
        throw new YamlException("Value of '" + path + "'is not a number");
    }
}
