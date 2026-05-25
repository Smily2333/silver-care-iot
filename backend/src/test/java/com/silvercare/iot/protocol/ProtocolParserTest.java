package com.silvercare.iot.protocol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtocolParserTest {

    private final ProtocolParser parser = new ProtocolParser();

    @Test
    void parsesHeartbeat() {
        ProtocolFrame frame = parser.parse("[3G*8800000015*000D*LK,50,100,100]");

        assertThat(frame.vendor()).isEqualTo("3G");
        assertThat(frame.deviceNo()).isEqualTo("8800000015");
        assertThat(frame.command()).isEqualTo("LK");
        assertThat(frame.content()).isEqualTo("LK,50,100,100");
    }

    @Test
    void rejectsLengthMismatch() {
        assertThatThrownBy(() -> parser.parse("[3G*8800000015*000E*LK,50,100,100]"))
                .isInstanceOf(ProtocolParseException.class)
                .hasMessageContaining("LEN mismatch");
    }

    @Test
    void buildsPacketWithHexLength() {
        assertThat(parser.build("3G", "8800000015", "LK"))
                .isEqualTo("[3G*8800000015*0002*LK]");
    }
}
