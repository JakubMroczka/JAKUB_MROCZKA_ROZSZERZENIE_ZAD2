package pl.kurs.fx;

import java.util.Objects;

public class CurrencyPair {
    private final String from;
    private final String to;

    public CurrencyPair(String from, String to) {
        if (from == null || from.isBlank()) throw new IllegalArgumentException("from blank");
        if (to == null || to.isBlank()) throw new IllegalArgumentException("to blank");
        this.from = from.toUpperCase();
        this.to = to.toUpperCase();
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CurrencyPair that)) return false;
        return from.equals(that.from) && to.equals(that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return from + "->" + to;
    }
}
