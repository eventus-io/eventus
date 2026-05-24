package io.eventus.streams.model;

import java.util.List;

public record StreamTopology(
        String applicationName,
        List<StreamBinding> bindings
) {}
