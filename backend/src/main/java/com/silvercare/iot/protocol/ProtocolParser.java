package com.silvercare.iot.protocol;

import java.nio.charset.StandardCharsets;

public class ProtocolParser {

    public ProtocolFrame parse(String rawPacket) {
        if (rawPacket == null || !rawPacket.startsWith("[") || !rawPacket.endsWith("]")) {
            throw new ProtocolParseException("Packet must be wrapped with [ and ]");
        }

        String body = rawPacket.substring(1, rawPacket.length() - 1);
        String[] parts = body.split("\\*", 4);
        if (parts.length != 4) {
            throw new ProtocolParseException("Packet must match vendor*deviceNo*len*content");
        }

        String vendor = parts[0];
        String deviceNo = parts[1];
        String lenHex = parts[2];
        String content = parts[3];

        if (lenHex.length() != 4) {
            throw new ProtocolParseException("LEN must be 4 hex characters");
        }

        int expectedLength;
        try {
            expectedLength = Integer.parseInt(lenHex, 16);
        } catch (NumberFormatException ex) {
            throw new ProtocolParseException("LEN is not valid hex: " + lenHex);
        }

        int actualLength = content.getBytes(StandardCharsets.US_ASCII).length;
        if (actualLength != expectedLength) {
            throw new ProtocolParseException("LEN mismatch, expected " + expectedLength + " but got " + actualLength);
        }

        String command = extractCommand(content);
        return new ProtocolFrame(vendor, deviceNo, lenHex, content, command, rawPacket);
    }

    public String build(String vendor, String deviceNo, String content) {
        String lenHex = String.format("%04X", content.getBytes(StandardCharsets.US_ASCII).length);
        return "[" + vendor + "*" + deviceNo + "*" + lenHex + "*" + content + "]";
    }

    private String extractCommand(String content) {
        int commaIndex = content.indexOf(',');
        return commaIndex >= 0 ? content.substring(0, commaIndex) : content;
    }
}
