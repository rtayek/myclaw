package com.ray.myclaw;

import java.util.Objects;

record BackendChoice(String id, String label) {
    BackendChoice {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
    }

    @Override
    public String toString() {
        return label;
    }
}
