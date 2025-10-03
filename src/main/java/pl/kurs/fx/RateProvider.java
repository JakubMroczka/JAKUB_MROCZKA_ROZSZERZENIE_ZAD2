package pl.kurs.fx;

public interface RateProvider {
    double getRate(String from, String to) throws Exception;
}