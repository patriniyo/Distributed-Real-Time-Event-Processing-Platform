package com.drep.dashboard.client;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProxyUriBuilder {

    private ProxyUriBuilder() {
    }

    public static String build(String path, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        params.forEach((key, value) -> {
            if (value != null) {
                builder.queryParam(key, value);
            }
        });
        return builder.build().toUriString();
    }

    public static Map<String, Object> params(Object... keyValues) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            params.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return params;
    }
}
