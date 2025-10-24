package org.examples.server.soap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import jakarta.xml.ws.Endpoint;

@Configuration
public class Publisher implements CommandLineRunner {
    private final HotelServiceImpl impl;
    public Publisher(HotelServiceImpl impl){ this.impl = impl; }

    @Override public void run(String... args) {
        String url = "http://0.0.0.0:8080/hotelservice";
        Endpoint.publish(url, impl);
        System.out.println("SOAP HotelService @ " + url + " (WSDL: " + url + "?wsdl)");
    }
}
