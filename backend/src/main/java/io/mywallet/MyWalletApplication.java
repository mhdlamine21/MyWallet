package io.mywallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MyWallet - simulated portfolio management and trading platform.
 *
 * <p><strong>Disclaimer:</strong> this is an educational simulator. No real orders are
 * ever executed, no real money is involved, and nothing in this application constitutes
 * financial advice.</p>
 */
@SpringBootApplication
@EnableScheduling // used by the market data generator (GBM price ticks) and the demo-data reset job
public class MyWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyWalletApplication.class, args);
    }
}
