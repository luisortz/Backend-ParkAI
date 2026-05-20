package com.parkai.backend.exception;

public class DuplicateFavoriteException
        extends RuntimeException {

    public DuplicateFavoriteException(
            String message
    ) {
        super(message);
    }
}