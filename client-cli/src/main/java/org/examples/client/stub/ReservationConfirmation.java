package org.examples.client.stub;

public class ReservationConfirmation {
    private String message;
    private String id;
    private Offer offer;
    public String getMessage(){ return message; }
    public void setMessage(String m){ this.message = m; }
    public String getId(){ return id; }
    public void setId(String i){ this.id = i; }
    public Offer getOffer(){ return offer; }
    public void setOffer(Offer o){ this.offer = o; }
}

