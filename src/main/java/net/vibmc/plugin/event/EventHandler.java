package net.vibmc.plugin.event;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
    int priority() default 0;
    boolean ignoreCancelled() default false;
}
