package io.eventus.streams.model;

public record StreamBinding(
        String name,
        String contentType,
        BindingType type,
        String topic,
        String group
) {}
