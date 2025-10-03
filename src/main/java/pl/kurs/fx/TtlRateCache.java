package pl.kurs.fx;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class TtlRateCache {

    private static final class Entry {
        final double value;
        final long expiresAtMillis;

        Entry(double value, long expiresAtMillis) {
            this.value = value;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private final ConcurrentMap<CurrencyPair, Entry> map = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final Clock clock;

    public TtlRateCache(long ttlMillis, Clock clock) {
        if (ttlMillis <= 0) throw new IllegalArgumentException("ttlMillis <= 0");
        this.ttlMillis = ttlMillis;
        this.clock = clock;
    }


    public Optional<Double> get(CurrencyPair key) {
        long now = clock.millis();
        Entry e = map.get(key);
        if (e == null) return Optional.empty();
        if (now >= e.expiresAtMillis) {
            map.remove(key, e);
            return Optional.empty();
        }
        return Optional.of(e.value);
    }

    public double get(CurrencyPair key, java.util.function.DoubleSupplier loader) {
        long now = clock.millis();
        Entry e = map.compute(key, (k, old) -> {
            if (old == null || now >= old.expiresAtMillis) {
                double value = loader.getAsDouble();
                return new Entry(value, now + ttlMillis);
            }
            return old;
        });
        return e.value;
    }


    public void put(CurrencyPair key, double value) {
        long expires = clock.millis() + ttlMillis;
        map.put(key, new Entry(value, expires));
    }

    public int size() {
        return map.size();
    }

    @Override
    public String toString() {
        return "TtlRateCache{ttl=" + ttlMillis + "ms, size=" + size() +
                ", now=" + Instant.ofEpochMilli(clock.millis()) + "}";
    }
}