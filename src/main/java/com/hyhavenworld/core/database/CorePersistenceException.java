package com.hyhavenworld.core.database;

public class CorePersistenceException extends RuntimeException {
    public CorePersistenceException() {
        super();
    }

    public CorePersistenceException(String message) {
        super(message);
    }

    public CorePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public CorePersistenceException(Throwable cause) {
        super(cause);
    }
}
