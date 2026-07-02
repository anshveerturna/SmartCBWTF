package com.smartcbwtf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Value("${app.timezone:Asia/Kolkata}")
    private String appTimezone;

    @Bean
    public Clock systemClock() {
        return Clock.system(ZoneId.of(appTimezone));
    }
}
