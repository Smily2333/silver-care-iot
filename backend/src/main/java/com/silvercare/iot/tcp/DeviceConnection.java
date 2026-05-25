package com.silvercare.iot.tcp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class DeviceConnection {

    private final Socket socket;
    private volatile String deviceNo;
    private volatile Instant lastSeenAt = Instant.now();

    public DeviceConnection(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getDeviceNo() {
        return deviceNo;
    }

    public void setDeviceNo(String deviceNo) {
        this.deviceNo = deviceNo;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void markSeen() {
        this.lastSeenAt = Instant.now();
    }

    public synchronized void send(String packet) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(packet.getBytes(StandardCharsets.US_ASCII));
        outputStream.flush();
    }

    public void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
