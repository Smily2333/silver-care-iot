package com.silvercare.iot.domain.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RawPacketLogMappingTest {

    @Test
    void longPacketFieldsUseExplicitLongTextColumns() throws Exception {
        assertThat(column("content").columnDefinition()).isEqualTo("LONGTEXT");
        assertThat(column("rawPacket").columnDefinition()).isEqualTo("LONGTEXT");
    }

    private Column column(String fieldName) throws Exception {
        return RawPacketLog.class.getDeclaredField(fieldName).getAnnotation(Column.class);
    }
}
