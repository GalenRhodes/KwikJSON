package com.galenrhodes.kwikjson;

import java.util.Objects;

public class CharHolder {
    public static final KwikProperties props = new KwikProperties("kwikjson.properties");
    public static final char           NINE  = props.getChar("p.char_nine");
    public static final char           ZERO  = props.getChar("p.char_zero");

    public char value;

    public CharHolder(char value)        { this.value = value; }

    public void append(StringBuilder sb) { sb.append(value); }

    public void appendAndSet(StringBuilder sb, char ch) {
        sb.append(value);
        value = ch;
    }

    public boolean equals1of(char... others) {
        for(char ch : others) if(value == ch) return true;
        return false;
    }

    public char get() { return value; }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public boolean equals(Object o) { return ((this == o) || ((o != null) && (getClass() == o.getClass()) && (value == ((CharHolder)o).value))); }

    @Override
    public String toString() { return String.valueOf(value); }

    public boolean is(char ch)              { return (value == ch); }

    public boolean isDigit()                { return isDigit(value); }

    public boolean isWhitespace()           { return Character.isWhitespace(value); }

    public boolean not(char ch)             { return (value != ch); }

    public boolean notDigit()               { return notDigit(value); }

    public boolean notWhitespace()          { return !Character.isWhitespace(value); }

    public void set(char ch)                { value = ch; }

    public void set(CharHolder ch)          { value = ch.value; }

    public static boolean isDigit(char ch)  { return ((ch >= ZERO) && (ch <= NINE)); }

    public static boolean notDigit(char ch) { return ((ch < ZERO) || (ch > NINE)); }
}
