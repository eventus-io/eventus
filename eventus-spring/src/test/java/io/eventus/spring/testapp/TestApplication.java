package io.eventus.spring.testapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.events.config.EventPublicationAutoConfiguration;

@SpringBootApplication(exclude = EventPublicationAutoConfiguration.class)
public class TestApplication {}
