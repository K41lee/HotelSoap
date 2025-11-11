package org.examples.client.stub;

import java.util.ArrayList;
import java.util.List;

public class Catalog {
    private Cities cities = new Cities();
    private Agencies agencies = new Agencies();
    public Cities getCities() { return cities; }
    public Agencies getAgencies() { return agencies; }
    public void setCities(Cities c) { this.cities = c; }
    public void setAgencies(Agencies a) { this.agencies = a; }
    public static class Cities { private List<String> city = new ArrayList<>(); public List<String> getCity(){ return city; } }
    public static class Agencies { private List<String> agency = new ArrayList<>(); public List<String> getAgency(){ return agency; } }
}

