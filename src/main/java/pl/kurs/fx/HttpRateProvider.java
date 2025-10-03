package pl.kurs.fx;

public class HttpRateProvider implements RateProvider {
    private final String apiKey;

    public HttpRateProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public double getRate(String from, String to) throws Exception {

        throw new UnsupportedOperationException("Not implemented in exam");
    }
}
