package grakn.common.collection;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

public final class Bytes {

    public static final int SHORT_SIZE = 2;
    public static final int INTEGER_SIZE = 4;
    public static final int LONG_SIZE = 8;
    public static final int DOUBLE_SIZE = 8;
    public static final int DATETIME_SIZE = LONG_SIZE;

    // TODO: convert HEX_ARRAY to byte[] once upgraded to Java 9+
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static byte[] join(byte[]... byteArrays) {
        int length = 0;
        for (byte[] array : byteArrays) {
            length += array.length;
        }

        byte[] joint = new byte[length];
        int pos = 0;
        for (byte[] array : byteArrays) {
            System.arraycopy(array, 0, joint, pos, array.length);
            pos += array.length;
        }

        return joint;
    }

    public static boolean bytesHavePrefix(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) return false;
        }
        return true;
    }

    public static byte[] shortToSortedBytes(int num) {
        byte[] bytes = new byte[SHORT_SIZE];
        bytes[1] = (byte) (num);
        bytes[0] = (byte) ((num >> 8) ^ 0x80);
        return bytes;
    }

    public static Short sortedBytesToShort(byte[] bytes) {
        assert bytes.length == SHORT_SIZE;
        bytes[0] = (byte) (bytes[0] ^ 0x80);
        return ByteBuffer.wrap(bytes).getShort();
    }

    public static byte[] integerToSortedBytes(int num) {
        byte[] bytes = new byte[INTEGER_SIZE];
        bytes[3] = (byte) (num);
        bytes[2] = (byte) (num >>= 8);
        bytes[1] = (byte) (num >>= 8);
        bytes[0] = (byte) ((num >> 8) ^ 0x80);
        return bytes;
    }

    public static long sortedBytesToInteger(byte[] bytes) {
        assert bytes.length == INTEGER_SIZE;
        bytes[0] = (byte) (bytes[0] ^ 0x80);
        return ByteBuffer.wrap(bytes).getInt();
    }

    public static byte[] longToSortedBytes(long num) {
        byte[] bytes = new byte[LONG_SIZE];
        bytes[7] = (byte) (num);
        bytes[6] = (byte) (num >>= 8);
        bytes[5] = (byte) (num >>= 8);
        bytes[4] = (byte) (num >>= 8);
        bytes[3] = (byte) (num >>= 8);
        bytes[2] = (byte) (num >>= 8);
        bytes[1] = (byte) (num >>= 8);
        bytes[0] = (byte) ((num >> 8) ^ 0x80);
        return bytes;
    }

    public static long sortedBytesToLong(byte[] bytes) {
        assert bytes.length == LONG_SIZE;
        bytes[0] = (byte) (bytes[0] ^ 0x80);
        return ByteBuffer.wrap(bytes).getLong();
    }

    /**
     * Convert {@code double} to lexicographically sorted bytes.
     *
     * We need to implement a custom byte representation of doubles. The bytes
     * need to be lexicographically sortable in the same order as the numerical
     * values of themselves. I.e. The bytes of -10 need to come before -1, -1
     * before 0, 0 before 1, and 1 before 10, and so on. This is not true with
     * the (default) 2's complement byte representation of doubles.
     *
     * We need to XOR all positive numbers with 0x8000... and XOR negative
     * numbers with 0xffff... This should flip the sign bit on both (so negative
     * numbers go first), and then reverse the ordering on negative numbers.
     *
     * @param value the {@code double} value to convert
     * @return the sorted byte representation of the {@code double} value
     */
    public static byte[] doubleToSortedBytes(double value) {
        byte[] bytes = ByteBuffer.allocate(DOUBLE_SIZE).putDouble(value).array();
        if (value >= 0) {
            bytes[0] = (byte) (bytes[0] ^ 0x80);
        } else {
            for (int i = 0; i < DOUBLE_SIZE; i++) {
                bytes[i] = (byte) (bytes[i] ^ 0xff);
            }
        }
        return bytes;
    }

    public static double sortedBytesToDouble(byte[] bytes) {
        assert bytes.length == DOUBLE_SIZE;
        if ((bytes[0] & 0x80) == 0x80) {
            bytes[0] = (byte) (bytes[0] ^ 0x80);
        } else {
            for (int i = 0; i < DOUBLE_SIZE; i++) {
                bytes[i] = (byte) (bytes[i] ^ 0xff);
            }
        }
        return ByteBuffer.wrap(bytes).getDouble();
    }

    public static byte[] stringToBytes(String value, Charset encoding) {
        byte[] bytes = value.getBytes(encoding);
        return join(new byte[]{(byte) bytes.length}, bytes);
    }

    public static String bytesToString(byte[] bytes, Charset encoding) {
        byte[] x = Arrays.copyOfRange(bytes, 1, 1 + bytes[0]);
        return new String(x, encoding);
    }

    /* s must be an even-length string. */
    public static byte[] hexStringToBytes(String s) {
        final int len = s.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String bytesToHexString(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        // TODO when hexChars is a byte[]: return new String(hexChars, StandardCharsets.UTF_8);
        return new String(hexChars);
    }

    public static byte booleanToByte(boolean value) {
        return (byte) (value ? 1 : 0);
    }

    public static Boolean byteToBoolean(byte aByte) {
        return aByte == 1;
    }

    public static byte[] dateTimeToBytes(java.time.LocalDateTime value, ZoneId timeZoneID) {
        return longToSortedBytes(value.atZone(timeZoneID).toInstant().toEpochMilli());
    }

    public static java.time.LocalDateTime bytesToDateTime(byte[] bytes, ZoneId timeZoneID) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(sortedBytesToLong(bytes)), timeZoneID);
    }

    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    public static UUID bytesToUUID(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long firstLong = buffer.getLong();
        long secondLong = buffer.getLong();
        return new UUID(firstLong, secondLong);
    }

    public static boolean arrayContains(byte[] container, int from, byte[] contained) {
        if ((container.length - from) > contained.length) return false;
        for (int i = 0; i < contained.length; i++) {
            if (container[from + i] != contained[i]) return false;
        }
        return true;
    }
}
