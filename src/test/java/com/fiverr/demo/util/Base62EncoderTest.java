package com.fiverr.demo.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    void testEncodeZero() {
        assertEquals("0", Base62Encoder.encode(0));
    }

    @Test
    void testEncodeSingleDigit() {
        assertEquals("1", Base62Encoder.encode(1));
        assertEquals("9", Base62Encoder.encode(9));
    }

    @Test
    void testEncodeDoubleDigit() {
        assertEquals("A", Base62Encoder.encode(10));
        assertEquals("Z", Base62Encoder.encode(35));
    }

    @Test
    void testEncodeLowercase() {
        assertEquals("a", Base62Encoder.encode(36));
        assertEquals("z", Base62Encoder.encode(61));
    }

    @Test
    void testEncodeMultipleCharacters() {
        assertEquals("10", Base62Encoder.encode(62));
        assertEquals("1Z", Base62Encoder.encode(97));
    }

    @Test
    void testEncodeLargeNumber() {
        String encoded = Base62Encoder.encode(Long.MAX_VALUE);
        assertNotNull(encoded);
        assertTrue(encoded.length() > 0);
        // Verify roundtrip for large numbers
        assertEquals(Long.MAX_VALUE, Base62Encoder.decode(encoded));
    }

    @Test
    void testDecodeZero() {
        assertEquals(0, Base62Encoder.decode("0"));
    }

    @Test
    void testDecodeSingleCharacter() {
        assertEquals(1, Base62Encoder.decode("1"));
        assertEquals(10, Base62Encoder.decode("A"));
        assertEquals(36, Base62Encoder.decode("a"));
    }

    @Test
    void testDecodeMultipleCharacters() {
        assertEquals(62, Base62Encoder.decode("10"));
        assertEquals(97, Base62Encoder.decode("1Z"));
    }

    @Test
    void testEncodeDecodeRoundtrip() {
        long[] testValues = {0, 1, 10, 62, 100, 1000, 10000, 100000, 1000000};
        for (long value : testValues) {
            String encoded = Base62Encoder.encode(value);
            long decoded = Base62Encoder.decode(encoded);
            assertEquals(value, decoded, "Failed for value: " + value);
        }
    }

    @Test
    void testDecodeInvalidCharacter() {
        assertThrows(IllegalArgumentException.class, () -> {
            Base62Encoder.decode("!@#");
        });
    }

    @Test
    void testDecodeInvalidCharacterInMiddle() {
        assertThrows(IllegalArgumentException.class, () -> {
            Base62Encoder.decode("A$B");
        });
    }
}
