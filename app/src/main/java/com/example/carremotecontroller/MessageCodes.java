package com.example.carremotecontroller;

public enum MessageCodes {
    MESSAGE_READ(0),
    MESSAGE_WRITE(1),
    MESSAGE_TOAST(2);

    private final int value;

    MessageCodes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
