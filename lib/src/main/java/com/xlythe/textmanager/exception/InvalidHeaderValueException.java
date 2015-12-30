package com.xlythe.textmanager.exception;

/**
 * Thrown when an invalid header value was set.
 */
public class InvalidHeaderValueException extends MmsException {
    /**
     * Constructs an InvalidHeaderValueException with no detailed message.
     */
    public InvalidHeaderValueException() {
        super();
    }
    /**
     * Constructs an InvalidHeaderValueException with the specified detailed message.
     *
     * @param message the detailed message.
     */
    public InvalidHeaderValueException(String message) {
        super(message);
    }
}
