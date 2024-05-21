package org.apache.nifi.util.hpe;

/**
 * HPE Configuration Exception indicating runtime failures
 */
public class HpePropertiesException extends RuntimeException {
    public HpePropertiesException(String message) {
        super(message);
    }

    public HpePropertiesException(String message, Throwable cause) {
        super(message, cause);
    }
}
