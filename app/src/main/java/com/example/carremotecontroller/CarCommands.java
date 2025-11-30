package com.example.carremotecontroller;

public enum CarCommands {
    FORWARDS(8),
    BACKWARDS(2),
    RIGHT(6),
    LEFT(4),
    STOP(5),
    TEST_ON(1),
    TEST_OFF(0);
    private final int value;

    CarCommands(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
