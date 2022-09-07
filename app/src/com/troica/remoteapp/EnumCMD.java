package com.troica.remoteapp;

public enum EnumCMD {
    NULL(-1),
    NOTHING(0),

    SEND_IMAGE(1000),

    CMD_MOUSE_DOWN(2001),
    CMD_MOUSE_UP(2002),
    CMD_MOUSE_MOVE(2003),
    CMD_MOUSE_CLICK(2004),
    CMD_MOUSE_DBL_CLICK(2005);

    private int value;

    public int getValue() {
        return value;
    }

    private EnumCMD(int value) {
        this.value = value;
    }
    public static EnumCMD fromInt(int val) {
        for (EnumCMD b : EnumCMD.values()) {
            if (b.value == val) {
                return b;
            }
        }
        return EnumCMD.NULL;
    }

    public static String getEnumName(int val) {
        return EnumCMD.fromInt(val).name();
    }

}
