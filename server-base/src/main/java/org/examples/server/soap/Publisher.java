package org.examples.server.soap;

import javax.xml.ws.Endpoint;
import org.examples.server.api.HotelAgencyService;
import org.examples.server.soap.HotelService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Publisher implements CommandLineRunner, DisposableBean {

    private final ApplicationContext ctx;
    private final Environment env;

    @Value("${server.port:8080}")
    private int port;

    @Value("${soap.path.defaultAgency:/agency}")
    private String defaultAgencyPath;

    @Value("${soap.path.defaultHotel:/hotel}")
    private String defaultHotelPath;

    private final List<Endpoint> published = new ArrayList<>();

    public Publisher(ApplicationContext ctx, Environment env) {
        this.ctx = ctx;
        this.env = env;
    }

    @Override
    public void run(String... args) {
        System.out.println("[SOAP] Publisher démarré — recherche des beans de service...");
        // Publish all HotelAgencyService beans
        Map<String, HotelAgencyService> agencyServices = ctx.getBeansOfType(HotelAgencyService.class);
        for (Map.Entry<String, HotelAgencyService> e : agencyServices.entrySet()) {
            String beanName = e.getKey();
            HotelAgencyService service = e.getValue();
            String perBeanPath = env.getProperty("soap.path." + beanName, defaultAgencyPath);
            if (!perBeanPath.startsWith("/")) perBeanPath = "/" + perBeanPath;
            String address = "http://0.0.0.0:" + port + perBeanPath;
            try {
                Endpoint ep = Endpoint.publish(address, service);
                published.add(ep);
                System.out.printf("[SOAP] HotelAgencyService bean '%s' publié sur %s?wsdl%n", beanName, address);
            } catch (Throwable t) {
                System.err.printf("[SOAP] Échec publication HotelAgencyService bean '%s' sur %s: %s%n", beanName, address, t.toString());
                t.printStackTrace(System.err);
            }
        }

        // Publish all HotelService beans
        Map<String, HotelService> hotelServices = ctx.getBeansOfType(HotelService.class);
        for (Map.Entry<String, HotelService> e : hotelServices.entrySet()) {
            String beanName = e.getKey();
            HotelService service = e.getValue();
            String perBeanPath = env.getProperty("soap.path." + beanName, defaultHotelPath);
            if (!perBeanPath.startsWith("/")) perBeanPath = "/" + perBeanPath;
            String address = "http://0.0.0.0:" + port + perBeanPath;
            try {
                Endpoint ep = Endpoint.publish(address, service);
                published.add(ep);
                System.out.printf("[SOAP] HotelService bean '%s' publié sur %s?wsdl%n", beanName, address);
            } catch (Throwable t) {
                System.err.printf("[SOAP] Échec publication HotelService bean '%s' sur %s: %s%n", beanName, address, t.toString());
                t.printStackTrace(System.err);
            }
        }

        if (agencyServices.isEmpty() && hotelServices.isEmpty()) {
            System.out.println("[SOAP] Aucun service SOAP Hotel trouvé à publier.");
        }
    }

    @Override
    public void destroy() {
        for (Endpoint ep : published) {
            try { ep.stop(); } catch (Exception ignore) {}
        }
        published.clear();
    }
}
