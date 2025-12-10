package com.hyhavenworld.core.util;

public class YamlException extends IllegalArgumentException{
    public YamlException() {
        super();
    }

    public YamlException(String message) {
        super(message);
    }

    public YamlException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlException(Throwable cause) {
        super(cause);
    }
}
