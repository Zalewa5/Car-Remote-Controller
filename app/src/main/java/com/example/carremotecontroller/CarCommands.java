package com.example.carremotecontroller;

public enum CarCommands {
    FORWARDS(8),
    BACKWARDS(2),
    RIGHT(6),
    LEFT(4),
    STOP(5),
    UPLEFT(7),
    UPRIGHT(9),
    DOWNLEFT(1),
    DOWNRIGHT(3),
    HONK(0);
    private final int value;

    CarCommands(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
