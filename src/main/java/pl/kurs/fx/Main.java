package pl.kurs.fx;

import java.time.Clock;

public class Main {
    public static void main(String[] args) {
        RateProvider provider = FxInitializer.rateProvider();
        TtlRateCache cache = FxInitializer.rateCache(10_000L, Clock.systemUTC());
        CurrencyService service = FxInitializer.currencyService(provider, cache);

        System.out.println("FX module wired (provider mocked in tests).");
    }
}