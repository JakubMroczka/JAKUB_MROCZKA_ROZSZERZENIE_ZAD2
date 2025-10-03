package pl.kurs.fx;

import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CurrencyServiceTest {


    private static class MutableClock extends Clock {
        private long millis;
        private final ZoneId zone = ZoneId.systemDefault();

        MutableClock(long millis) {
            this.millis = millis;
        }

        void plusMillis(long add) {
            this.millis += add;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public long millis() {
            return millis;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }
    }

    @Test
    public void shouldUseCacheWithinTtl() throws Exception {

        RateProvider provider = mock(RateProvider.class);
        MutableClock clock = new MutableClock(1_000_000L);
        TtlRateCache cache = new TtlRateCache(10_000, clock); // TTL = 10s
        CurrencyService service = new CurrencyService(provider, cache);

        when(provider.getRate("USD", "PLN")).thenReturn(4.00);


        double a1 = service.exchange("USD", "PLN", 10);
        double a2 = service.exchange("USD", "PLN", 5);


        assertEquals(40.0, a1, 1e-9);
        assertEquals(20.0, a2, 1e-9);
        verify(provider, times(1)).getRate("USD", "PLN");
    }

    @Test
    public void shouldRefreshAfterTtlExpires() throws Exception {
        RateProvider provider = mock(RateProvider.class);
        when(provider.getRate("USD", "PLN")).thenReturn(4.0, 4.1);

        MutableClock clock = new MutableClock(System.currentTimeMillis());
        TtlRateCache cache = new TtlRateCache(10_000, clock);
        CurrencyService service = new CurrencyService(provider, cache);

        assertEquals(40.0, service.exchange("USD", "PLN", 10), 1e-9);


        clock.plusMillis(10_001);


        assertEquals(41.0, service.exchange("USD", "PLN", 10), 1e-9);

        verify(provider, times(2)).getRate("USD", "PLN");
    }

    @Test
    public void shouldBeThreadSafe_underParallelCalls() throws Exception {
        RateProvider provider = mock(RateProvider.class);
        when(provider.getRate("GBP", "PLN")).thenReturn(5.0);


        MutableClock clock = new MutableClock(System.currentTimeMillis());
        TtlRateCache cache = new TtlRateCache(1_000_000, clock);
        CurrencyService service = new CurrencyService(provider, cache);

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Double>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> service.exchange("GBP", "PLN", 2.0)));
        }

        for (Future<Double> f : futures) {
            assertEquals(10.0, f.get(), 1e-9);
        }
        pool.shutdown();

        verify(provider, atMost(2)).getRate("GBP", "PLN");
    }


    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeAmount() throws Exception {
        RateProvider provider = mock(RateProvider.class);
        TtlRateCache cache = new TtlRateCache(10_000, Clock.systemUTC());
        CurrencyService service = new CurrencyService(provider, cache);

        service.exchange("USD", "PLN", -1);
    }

    @Test
    public void shouldCacheSeparatelyPerPair() throws Exception {
        RateProvider p = mock(RateProvider.class);
        when(p.getRate("GBP", "PLN")).thenReturn(5.0);
        when(p.getRate("EUR", "PLN")).thenReturn(4.0);

        TtlRateCache cache = new TtlRateCache(10_000, Clock.systemUTC());
        CurrencyService s = new CurrencyService(p, cache);

        assertEquals(50.0, s.exchange("GBP", "PLN", 10), 1e-9);
        assertEquals(40.0, s.exchange("EUR", "PLN", 10), 1e-9);

        assertEquals(50.0, s.exchange("GBP", "PLN", 10), 1e-9);
        assertEquals(40.0, s.exchange("EUR", "PLN", 10), 1e-9);

        verify(p, times(1)).getRate("GBP", "PLN");
        verify(p, times(1)).getRate("EUR", "PLN");
    }


    @Test
    public void shouldNormalizeCodesToUppercase() throws Exception {
        RateProvider p = mock(RateProvider.class);
        when(p.getRate("USD", "PLN")).thenReturn(4.0);

        TtlRateCache cache = new TtlRateCache(10_000, Clock.systemUTC());
        CurrencyService s = new CurrencyService(p, cache);

        assertEquals(8.0, s.exchange("usd", "pln", 2.0), 1e-9);

        verify(p, times(1)).getRate("USD", "PLN");
    }


    @Test(expected = ExchangeRateFetchException.class)
    public void shouldNotPolluteCacheWhenProviderFails() throws Exception {
        RateProvider p = mock(RateProvider.class);
        when(p.getRate("USD", "PLN")).thenThrow(new RuntimeException("boom"));

        TtlRateCache cache = new TtlRateCache(10_000, Clock.systemUTC());
        CurrencyService s = new CurrencyService(p, cache);

        try {
            s.exchange("USD", "PLN", 1);
        } finally {
            assertTrue(cache.get(new CurrencyPair("USD", "PLN")).isEmpty());
        }
    }

    @org.junit.Test(expected = IllegalArgumentException.class)
    public void ttlRateCache_shouldRejectNonPositiveTtl() {
        new TtlRateCache(0, java.time.Clock.systemUTC());
    }


    @org.junit.Test(expected = IllegalArgumentException.class)
    public void currencyPair_shouldRejectNullFrom() {
        new CurrencyPair(null, "PLN");
    }

    @org.junit.Test(expected = IllegalArgumentException.class)
    public void currencyPair_shouldRejectBlankFrom() {
        new CurrencyPair("   ", "PLN");
    }

    @org.junit.Test(expected = IllegalArgumentException.class)
    public void currencyPair_shouldRejectNullTo() {
        new CurrencyPair("USD", null);
    }

    @org.junit.Test(expected = IllegalArgumentException.class)
    public void currencyPair_shouldRejectBlankTo() {
        new CurrencyPair("USD", "  ");
    }
}