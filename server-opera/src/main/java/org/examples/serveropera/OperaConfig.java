package org.examples.serveropera;

import Impl.DataFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OperaConfig {
    @Bean
    public DataFactory dataFactory() {
        return DataFactory.opera();
    }
}
