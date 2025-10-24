package org.examples.server.dto;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class Room {
    public int numero;
    public int nbLits;
    public int prixParNuit;
}
