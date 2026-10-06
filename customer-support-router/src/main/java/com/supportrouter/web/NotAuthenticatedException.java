package com.supportrouter.web;

class NotAuthenticatedException extends RuntimeException {
    NotAuthenticatedException(String message) {
        super(message);
    }
}
