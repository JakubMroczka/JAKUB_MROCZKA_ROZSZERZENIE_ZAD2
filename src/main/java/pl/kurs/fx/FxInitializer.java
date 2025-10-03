package pl.kurs.fx;

import java.time.Clock;

public class FxInitializer {
    private FxInitializer() {
    }

    public static TtlRateCache rateCache(long ttlMillis, Clock clock) {
        return new TtlRateCache(ttlMillis, clock);
    }

    public static RateProvider rateProvider() {
        return (from, to) -> {
            throw new UnsupportedOperationException("HTTP not required here");
        };
    }

    public static CurrencyService currencyService(RateProvider provider, TtlRateCache cache) {
        return new CurrencyService(provider, cache);
    }
}
