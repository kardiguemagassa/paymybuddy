package com.openclassrooms.paymybuddy.exception;

public class EmailExistException extends Exception {
    public EmailExistException(String message) {
        super(message);
    }
}
