package org.examples.server.dto;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "Catalog")
@XmlType(name = "Catalog", namespace = "http://service.hotel.examples.org/dto")
@XmlAccessorType(XmlAccessType.FIELD)
public class Catalog {
    @XmlElementWrapper(name = "cities")
    @XmlElement(name = "city")
    public List<String> cities = new ArrayList<>();

    @XmlElementWrapper(name = "agencies")
    @XmlElement(name = "agency")
    public List<String> agencies = new ArrayList<>();
}
