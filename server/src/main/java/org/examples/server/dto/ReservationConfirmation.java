package org.examples.server.dto;

import jakarta.xml.bind.annotation.*;

@XmlRootElement(name="ReservationConfirmation")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReservationConfirmation {
    public String id;
    public String message;
    public Offer offer;
}
