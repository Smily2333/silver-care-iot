package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.RawPacketLog;
import com.silvercare.iot.repository.RawPacketLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/raw-packets")
public class AdminPacketController {

    private final RawPacketLogRepository repository;

    public AdminPacketController(RawPacketLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<RawPacketLog> list(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
