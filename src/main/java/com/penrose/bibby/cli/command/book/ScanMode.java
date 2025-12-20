package com.penrose.bibby.cli.command.book;

public enum ScanMode {
    NONE,
    SINGLE,
    MULTI;

    public static ScanMode from(boolean scan, boolean multi) {
        if (!scan) return NONE;
        return multi ? MULTI : SINGLE;
    }
}
