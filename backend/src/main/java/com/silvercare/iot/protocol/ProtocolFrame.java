package com.silvercare.iot.protocol;

public record ProtocolFrame(
        String vendor,
        String deviceNo,
        String lenHex,
        String content,
        String command,
        String rawPacket
) {
}
