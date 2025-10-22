package org.examples.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
                "org.examples.hotel",
                "cli",
                "Impl"
        }
)
public class HotelCliApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotelCliApplication.class, args);
    }
}
