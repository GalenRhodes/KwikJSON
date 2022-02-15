package com.galenrhodes.kwikjson;

import java.util.Objects;
import java.util.concurrent.Callable;

public class BoolHolder {
    public boolean value;

    public BoolHolder(boolean value) { this.value = value; }

    public <T> T cond(Callable<T> whenTrue, Callable<T> whenFalse) throws KwikJSONException {
        try { return (value ? whenTrue.call() : whenFalse.call()); }
        catch(Exception e) { throw new KwikJSONException(e); }
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public boolean equals(Object o) { return ((this == o) || ((o != null) && (getClass() == o.getClass()) && (value == ((BoolHolder)o).value))); }

    @Override
    public String toString() { return (value ? "true" : "false"); }

    public boolean is()  { return value; }

    public boolean not() { return !value; }

    @SuppressWarnings("UnusedReturnValue")
    public boolean set() {
        value = true;
        return value;
    }
}
