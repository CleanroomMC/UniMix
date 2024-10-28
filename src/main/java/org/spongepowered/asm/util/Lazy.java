package org.spongepowered.asm.util;

import java.util.function.Supplier;

public final class Lazy {

    public static Lazy of(Object value) {
        return new Lazy(value);
    }

    private Object value;

    private Lazy(Object value) {
        this.value = value;
    }

    public <T> T get() {
        if (value instanceof Supplier) {
            value = ((Supplier) value).get();
        }
        return (T) value;
    }

}
