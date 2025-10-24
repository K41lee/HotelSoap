package org.examples.server.soap;

import jakarta.xml.ws.WebFault;

@WebFault(name="ServiceFault")
public class ServiceFault extends Exception {
    public ServiceFault(String message){ super(message); }
}
