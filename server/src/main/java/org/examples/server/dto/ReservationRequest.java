package org.examples.server.dto;

import jakarta.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "ReservationRequest",
        namespace = "http://service.hotel.examples.org/dto",
        propOrder = { "hotelName", "roomNumber", "nom", "prenom", "carte", "agence", "arrivee", "depart" }
)
@XmlRootElement(name = "ReservationRequest")
public class ReservationRequest {
    public String hotelName;
    public int roomNumber;
    public String nom, prenom, carte;

    @XmlElement(name = "agence", required = false, nillable = true)
    public String agence;

    @XmlSchemaType(name="date") public XMLGregorianCalendar arrivee;
    @XmlSchemaType(name="date") public XMLGregorianCalendar depart;
}
