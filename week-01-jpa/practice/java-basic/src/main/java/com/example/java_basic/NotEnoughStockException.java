package com.example.java_basic;

public class NotEnoughStockException extends RuntimeException{
    public NotEnoughStockException(String message) { super(message); }
}
