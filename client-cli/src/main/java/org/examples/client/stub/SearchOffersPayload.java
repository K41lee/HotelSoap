package org.examples.client.stub;

import java.util.ArrayList;
import java.util.List;

public class SearchOffersPayload {
    private List<Offer> offer = new ArrayList<>();
    public List<Offer> getOffer(){ return offer; }
    public void setOffer(List<Offer> list){ this.offer = list; }
}

