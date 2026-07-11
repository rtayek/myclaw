package com.ray.myclaw;

final class CommandExecutionException extends RuntimeException {
    CommandExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    CommandExecutionException(String message) {
        super(message);
    }
}
