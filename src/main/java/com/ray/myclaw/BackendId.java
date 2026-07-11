package com.ray.myclaw;

import java.util.Objects;

record BackendId(String value) {
    BackendId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
