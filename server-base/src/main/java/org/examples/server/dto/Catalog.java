package org.examples.server.dto;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="CatalogDTO", namespace="http://service.hotel.examples.org/dto")
@XmlRootElement(name="CatalogDTO", namespace="http://service.hotel.examples.org/dto")
public class Catalog {
    // nom de l'hôtel exposé
    public String name;

    private Cities cities = new Cities();
    private Agencies agencies = new Agencies();
    public Cities getCities() { return cities; }
    public Agencies getAgencies() { return agencies; }
    public void setCities(Cities c) { this.cities = c; }
    public void setAgencies(Agencies a) { this.agencies = a; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public static class Cities { private List<String> city = new ArrayList<>(); public List<String> getCity(){ return city; } }
    public static class Agencies { private List<String> agency = new ArrayList<>(); public List<String> getAgency(){ return agency; } }
}
