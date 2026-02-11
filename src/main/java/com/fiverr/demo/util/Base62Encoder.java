package com.fiverr.demo.util;

public class Base62Encoder {
    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;

    public static String encode(long id) {
        if (id == 0) {
            return "0";
        }

        StringBuilder encoded = new StringBuilder();
        while (id > 0) {
            int remainder = (int) (id % BASE);
            encoded.insert(0, BASE62_CHARS.charAt(remainder));
            id = id / BASE;
        }
        return encoded.toString();
    }

    public static long decode(String shortCode) {
        long decoded = 0;
        for (int i = 0; i < shortCode.length(); i++) {
            char c = shortCode.charAt(i);
            int value = BASE62_CHARS.indexOf(c);
            if (value == -1) {
                throw new IllegalArgumentException("Invalid character in short code: " + c);
            }
            decoded = decoded * BASE + value;
        }
        return decoded;
    }
}
