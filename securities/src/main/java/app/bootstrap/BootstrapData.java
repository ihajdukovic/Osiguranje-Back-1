package app.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapData implements CommandLineRunner {

    private final StocksBootstrap stocksBootstrap;
    private final FuturesBootstrap futuresBootstrap;
    private final ForexBootstrap forexBootstrap;
    private final ExchangeBootstrap exchangeBootstrap;
    private final CurrencyBootstrap currencyBootstrap;

    @Autowired
    public BootstrapData(StocksBootstrap stocksBootstrap, FuturesBootstrap futuresBootstrap, ForexBootstrap forexBootstrap, ExchangeBootstrap exchangeBootstrap, CurrencyBootstrap currencyBootstrap) {
        this.stocksBootstrap = stocksBootstrap;
        this.futuresBootstrap = futuresBootstrap;
        this.forexBootstrap = forexBootstrap;
        this.exchangeBootstrap = exchangeBootstrap;
        this.currencyBootstrap = currencyBootstrap;
    }

    @Override
    public void run(String... args) throws Exception {
        currencyBootstrap.loadCurrenciesData();
        forexBootstrap.loadForexData();
        exchangeBootstrap.loadStockExchangeData();
        stocksBootstrap.loadStocksData();
        futuresBootstrap.loadFuturesData();
    }

}