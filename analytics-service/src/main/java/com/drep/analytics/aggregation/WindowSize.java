package com.drep.analytics.aggregation;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

public enum WindowSize {
    ONE_MINUTE("1m", Duration.ofMinutes(1)),
    FIVE_MINUTES("5m", Duration.ofMinutes(5)),
    ONE_HOUR("1hr", Duration.ofHours(1));

    private final String code;
    private final Duration duration;

    WindowSize(String code, Duration duration) {
        this.code = code;
        this.duration = duration;
    }

    public String code() {
        return code;
    }

    public Duration duration() {
        return duration;
    }

    public static Optional<WindowSize> fromCode(String code) {
        return Arrays.stream(values())
                .filter(w -> w.code.equalsIgnoreCase(code))
                .findFirst();
    }

    public static WindowSize[] all() {
        return values();
    }
}
