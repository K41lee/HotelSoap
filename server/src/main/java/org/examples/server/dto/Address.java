package org.examples.server.dto;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class Address {
    public String pays, ville, rue, lieuDit;
    public int numero;
    public double lat, lon;
}
