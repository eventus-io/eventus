package io.eventus.spring.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.events.config.EventPublicationAutoConfiguration;

@SpringBootApplication(exclude = EventPublicationAutoConfiguration.class)
public class TestApplication {}
