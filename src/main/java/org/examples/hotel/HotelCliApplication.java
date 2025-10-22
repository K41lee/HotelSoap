package org.examples.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// ajoute tes packages racine ici :
@SpringBootApplication(
        scanBasePackages = {
                "org.examples.hotel", // ton main + éventuellement Demo
                "cli",                // où se trouvent CliRunner / DataFactory
                "Impl"                // si tu as des @Component/@Configuration là-dedans
        }
)
public class HotelCliApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotelCliApplication.class, args);
    }
}
