package com.silvercare.iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SilverCareIotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SilverCareIotApplication.class, args);
    }
}
