package org.examples.serverrivage;

import Impl.DataFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RivageConfig {
    @Bean
    public DataFactory dataFactory() {
        return DataFactory.rivage(); // fabrique l’hôtel Rivage, ses chambres/agences
    }
}
