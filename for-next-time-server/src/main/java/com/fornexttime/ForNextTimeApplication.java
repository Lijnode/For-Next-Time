package com.fornexttime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ForNextTimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForNextTimeApplication.class, args);
    }
}
