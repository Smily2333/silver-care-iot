package com.silvercare.iot.service;

import com.silvercare.iot.domain.PacketDirection;
import com.silvercare.iot.domain.ParseStatus;
import com.silvercare.iot.domain.entity.RawPacketLog;
import com.silvercare.iot.protocol.ProtocolFrame;
import com.silvercare.iot.repository.RawPacketLogRepository;
import org.springframework.stereotype.Service;

@Service
public class RawPacketLogService {

    private final RawPacketLogRepository repository;

    public RawPacketLogService(RawPacketLogRepository repository) {
        this.repository = repository;
    }

    public RawPacketLog saveSuccess(ProtocolFrame frame) {
        RawPacketLog log = new RawPacketLog();
        log.setDeviceNo(frame.deviceNo());
        log.setDirection(PacketDirection.UPLINK);
        log.setCommand(frame.command());
        log.setLenHex(frame.lenHex());
        log.setContent(frame.content());
        log.setRawPacket(frame.rawPacket());
        log.setParseStatus(ParseStatus.SUCCESS);
        return repository.save(log);
    }

    public void saveFailure(String rawPacket, String errorMessage) {
        RawPacketLog log = new RawPacketLog();
        log.setDirection(PacketDirection.UPLINK);
        log.setRawPacket(rawPacket);
        log.setParseStatus(ParseStatus.FAILED);
        log.setErrorMessage(errorMessage);
        repository.save(log);
    }
}
