package com.silvercare.iot.geo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

import static org.assertj.core.api.Assertions.assertThat;

class NominatimClientConstructionTest {

    @Test
    void exposesAnUnambiguousSpringConstructor() {
        assertThat(BeanUtils.getResolvableConstructor(NominatimClient.class))
                .isEqualTo(NominatimClient.class.getConstructors()[0]);
    }
}
