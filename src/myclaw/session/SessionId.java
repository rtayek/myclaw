package myclaw.session;

import java.util.Objects;
import java.util.UUID;

public record SessionId(String value) {
    public SessionId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static SessionId create() {
        return new SessionId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
