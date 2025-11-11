package org.examples.server.tools;

import org.examples.server.api.HotelAgencyService;
import org.examples.server.soap.HotelService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BeanDumper implements CommandLineRunner {

    @Autowired
    private ApplicationContext ctx;

    @Override
    public void run(String... args) {
        try {
            System.out.println("[BEAN-DUMPER] Listing beans in ApplicationContext...");

            String[] names = ctx.getBeanDefinitionNames();
            Arrays.sort(names, Comparator.naturalOrder());
            System.out.printf("[BEAN-DUMPER] Total beans: %d\n", names.length);

            // print a short list (first 200 names)
            for (int i = 0; i < Math.min(names.length, 200); i++) {
                System.out.println("[BEAN-DUMPER] bean[" + i + "] = " + names[i]);
            }

            // Beans implementing HotelAgencyService
            Map<String, HotelAgencyService> agencies = ctx.getBeansOfType(HotelAgencyService.class);
            if (agencies.isEmpty()) {
                System.out.println("[BEAN-DUMPER] No beans implementing HotelAgencyService found.");
            } else {
                System.out.println("[BEAN-DUMPER] HotelAgencyService beans:");
                agencies.forEach((k,v) -> System.out.println("[BEAN-DUMPER]   " + k + " -> " + v.getClass().getName()));
            }

            // Beans implementing HotelService
            Map<String, HotelService> hotels = ctx.getBeansOfType(HotelService.class);
            if (hotels.isEmpty()) {
                System.out.println("[BEAN-DUMPER] No beans implementing HotelService found.");
            } else {
                System.out.println("[BEAN-DUMPER] HotelService beans:");
                hotels.forEach((k,v) -> System.out.println("[BEAN-DUMPER]   " + k + " -> " + v.getClass().getName()));
            }

            System.out.println("[BEAN-DUMPER] End of bean dump.");
        } catch (Throwable t) {
            System.err.println("[BEAN-DUMPER] Failed to dump beans: " + t);
            t.printStackTrace(System.err);
        }
    }
}

