package org.examples.server.dto;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "SearchOffersPayload")
@XmlType(
        name = "SearchOffersPayload",
        namespace = "http://service.hotel.examples.org/dto"
)
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchOffersResponse {
    @XmlElement(name="offer")
    public List<Offer> offers;
}
