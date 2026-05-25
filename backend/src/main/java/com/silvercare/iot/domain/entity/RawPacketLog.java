package com.silvercare.iot.domain.entity;

import com.silvercare.iot.domain.PacketDirection;
import com.silvercare.iot.domain.ParseStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "raw_packet_logs", indexes = {
        @Index(name = "idx_raw_packet_device_time", columnList = "deviceNo,receivedAt"),
        @Index(name = "idx_raw_packet_command", columnList = "command")
})
public class RawPacketLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String deviceNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PacketDirection direction;

    @Column(length = 32)
    private String command;

    @Column(length = 4)
    private String lenHex;

    @Lob
    private String content;

    @Lob
    @Column(nullable = false)
    private String rawPacket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ParseStatus parseStatus;

    @Column(length = 1024)
    private String errorMessage;

    @Column(nullable = false)
    private Instant receivedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getDeviceNo() {
        return deviceNo;
    }

    public void setDeviceNo(String deviceNo) {
        this.deviceNo = deviceNo;
    }

    public PacketDirection getDirection() {
        return direction;
    }

    public void setDirection(PacketDirection direction) {
        this.direction = direction;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getLenHex() {
        return lenHex;
    }

    public void setLenHex(String lenHex) {
        this.lenHex = lenHex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRawPacket() {
        return rawPacket;
    }

    public void setRawPacket(String rawPacket) {
        this.rawPacket = rawPacket;
    }

    public ParseStatus getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(ParseStatus parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
