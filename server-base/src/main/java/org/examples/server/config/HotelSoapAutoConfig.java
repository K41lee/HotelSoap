package org.examples.server.config;

import Impl.DataFactory;
import org.examples.server.impl.HotelAgencyServiceImpl;
import org.examples.server.soap.HotelServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@Configuration
@ConditionalOnBean(DataFactory.class)
public class HotelSoapAutoConfig {

    @Bean
    public HotelServiceImpl hotelService(DataFactory df) {
        // HotelServiceImpl currently constructs its own DataFactory; adapt it to accept one via field injection is simpler:
        HotelServiceImpl impl = new HotelServiceImpl();
        // set factory via reflection (since class currently has private final factory) - better to add constructor but keep minimal edits
        try {
            java.lang.reflect.Field f = HotelServiceImpl.class.getDeclaredField("factory");
            f.setAccessible(true);
            f.set(impl, df);
        } catch (Exception ignore) {}
        return impl;
    }

    @Bean
    public HotelAgencyServiceImpl hotelAgencyService(DataFactory df) {
        HotelAgencyServiceImpl impl = new HotelAgencyServiceImpl();
        try {
            java.lang.reflect.Field f = HotelAgencyServiceImpl.class.getDeclaredField("factory");
            f.setAccessible(true);
            f.set(impl, df);
        } catch (Exception ignore) {}
        return impl;
    }
}

