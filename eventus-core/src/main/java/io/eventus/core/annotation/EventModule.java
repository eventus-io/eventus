package io.eventus.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an Eventus module. Used by {@code AnnotationBasedExtractor}
 * to discover modules without a Spring or Modulith dependency.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface EventModule {
    /** Logical module name used as the node ID. Defaults to the simple class name when blank. */
    String name() default "";
}
