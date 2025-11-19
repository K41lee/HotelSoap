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

import java.lang.reflect.Method;
import java.util.*;

@Component
public class Publisher implements CommandLineRunner, DisposableBean {

    private final ApplicationContext ctx;
    private final Environment env;

    @Value("${server.port:8080}")
    private int webPort;

    @Value("${soap.port:${server.port:8080}}")
    private int soapPort;

    @Value("${soap.path.defaultAgency:/agency}")
    private String defaultAgencyPath;

    @Value("${soap.path.defaultHotel:/hotel}")
    private String defaultHotelPath;

    private final List<Endpoint> published = new ArrayList<>();

    public Publisher(ApplicationContext ctx, Environment env) {
        this.ctx = ctx;
        this.env = env;
    }

    private static String joinPath(String base, String suffix) {
        if (base == null || base.isEmpty()) return suffix.startsWith("/") ? suffix : "/" + suffix;
        String b = base.endsWith("/") ? base.substring(0, base.length()-1) : base;
        String s = suffix.startsWith("/") ? suffix : "/" + suffix;
        return b + s;
    }

    @Override
    public void run(String... args) {
        System.out.println("[SOAP] Publisher démarré — recherche des beans de service...");

        // module-level base path if defined (e.g. soap.path=/hotel-rivage in application.properties)
        String modulePath = env.getProperty("soap.path");

        // Publish all HotelAgencyService beans
        Map<String, HotelAgencyService> agencyServices = ctx.getBeansOfType(HotelAgencyService.class);
        for (Map.Entry<String, HotelAgencyService> e : agencyServices.entrySet()) {
            String beanName = e.getKey();
            HotelAgencyService service = e.getValue();
            // precedence: soap.path.<beanName> -> modulePath+/agency -> defaultAgencyPath
            String perBeanPath = env.getProperty("soap.path." + beanName);
            if (perBeanPath == null) perBeanPath = (modulePath != null ? joinPath(modulePath, "/agency") : defaultAgencyPath);
            if (!perBeanPath.startsWith("/")) perBeanPath = "/" + perBeanPath;
            String address = "http://0.0.0.0:" + soapPort + perBeanPath;
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
        System.out.println("[DIAG] HotelService beans détectés: " + hotelServices.keySet());

        // Try to find any Gestionnaire beans to inject into service instances (if they expose setGestionnaire)
        Map<String, Object> allBeans = ctx.getBeansOfType(Object.class);
        List<Object> foundGestionnaires = new ArrayList<>();
        for (Map.Entry<String, Object> e : allBeans.entrySet()) {
            Object val = e.getValue();
            if (val != null && val.getClass().getSimpleName().equals("Gestionnaire")) {
                foundGestionnaires.add(val);
            }
        }
        if (!foundGestionnaires.isEmpty()) {
            System.out.printf("[INIT] Found %d Gestionnaire bean(s) — will attempt to inject into HotelService instances%n", foundGestionnaires.size());
        }

        for (Map.Entry<String, HotelService> e : hotelServices.entrySet()) {
            String beanName = e.getKey();
            HotelService service = e.getValue();
            // precedence: soap.path.<beanName> -> modulePath+/hotel -> defaultHotelPath
            String perBeanPath = env.getProperty("soap.path." + beanName);
            if (perBeanPath == null) perBeanPath = (modulePath != null ? joinPath(modulePath, "/hotel") : defaultHotelPath);
            if (!perBeanPath.startsWith("/")) perBeanPath = "/" + perBeanPath;
            String address = "http://0.0.0.0:" + soapPort + perBeanPath;
            try {
                if (!foundGestionnaires.isEmpty()) {
                    Object toInject = foundGestionnaires.get(0);
                    try {
                        Method m = service.getClass().getMethod("setGestionnaire", toInject.getClass());
                        m.invoke(service, toInject);
                        System.out.printf("[INIT] Injected Gestionnaire bean into HotelService bean '%s' via setGestionnaire(%s)%n", beanName, toInject.getClass().getName());
                    } catch (NoSuchMethodException nsme) {
                        for (Method m2 : service.getClass().getMethods()) {
                            if (m2.getName().equals("setGestionnaire") && m2.getParameterCount() == 1 && m2.getParameterTypes()[0].isAssignableFrom(toInject.getClass())) {
                                m2.invoke(service, toInject);
                                System.out.printf("[INIT] Injected Gestionnaire bean into HotelService bean '%s' via setGestionnaire(%s)%n", beanName, toInject.getClass().getName());
                                break;
                            }
                        }
                    }
                }

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
