package pl.kurs.fx;



public class CurrencyService {
    private final RateProvider provider;
    private final TtlRateCache cache;

    public CurrencyService(RateProvider provider, TtlRateCache cache) {
        this.provider = provider;
        this.cache = cache;
    }

    public double exchange(String currencyFrom, String currencyTo, double amount) {
        if (amount < 0) throw new IllegalArgumentException("amount < 0");

        CurrencyPair pair = new CurrencyPair(currencyFrom, currencyTo);

        double rate = cache.get(pair, () -> {
            try {
                return provider.getRate(pair.from(), pair.to());
            } catch (Exception e) {
                throw new ExchangeRateFetchException(
                        "Failed to fetch rate " + pair.from() + "->" + pair.to(), e);
            }
        });

        return amount * rate;
    }
}