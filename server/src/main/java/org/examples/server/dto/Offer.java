package org.examples.server.dto;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class Offer {
    public String hotelName;
    public Address address;
    public String categorie;
    public int nbEtoiles;
    public Room room;
    public int prixTotal;          // après réduction éventuelle
    public String agenceApplied;   // null si aucune
}
