package io.eventus.streams.config;

import io.eventus.streams.model.BindingType;
import io.eventus.streams.model.StreamBinding;
import io.eventus.streams.model.StreamTopology;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaBindingReader {

    private static final String BINDINGS_PREFIX = "spring.cloud.stream.bindings.";

    private final ConfigurableEnvironment environment;

    public KafkaBindingReader(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    public StreamTopology readTopology(String appName) {
        Map<String, String> destinations = new HashMap<>();

        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String key : enumerable.getPropertyNames()) {
                    if (key.startsWith(BINDINGS_PREFIX) && key.endsWith(".destination")) {
                        String bindingName = key.substring(BINDINGS_PREFIX.length(),
                                key.length() - ".destination".length());
                        Object value = enumerable.getProperty(key);
                        if (value != null) {
                            destinations.put(bindingName, value.toString());
                        }
                    }
                }
            }
        }

        List<StreamBinding> bindings = new ArrayList<>();
        destinations.forEach((name, destination) -> {
            BindingType type = inferBindingType(name);
            String group = type == BindingType.INPUT ? inferGroup(name) : null;
            bindings.add(new StreamBinding(name, "application/json", type, destination, group));
        });

        return new StreamTopology(appName, bindings);
    }

    private BindingType inferBindingType(String bindingName) {
        return bindingName.endsWith("-in-0") || bindingName.endsWith("-in")
                ? BindingType.INPUT : BindingType.OUTPUT;
    }

    private String inferGroup(String bindingName) {
        if (bindingName.endsWith("-in-0")) return bindingName.replace("-in-0", "");
        if (bindingName.endsWith("-in")) return bindingName.replace("-in", "");
        return bindingName;
    }
}
