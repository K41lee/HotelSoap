package org.examples.client.stub;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;

public class ReservationRequest {
    private String hotelName;
    private int roomNumber;
    private XMLGregorianCalendar arrivee;
    private XMLGregorianCalendar depart;
    private String nom, prenom, carte;
    private JAXBElement<String> agence;

    public void setHotelName(String s){ this.hotelName = s; }
    public void setRoomNumber(int n){ this.roomNumber = n; }
    public void setArrivee(XMLGregorianCalendar d){ this.arrivee = d; }
    public void setDepart(XMLGregorianCalendar d){ this.depart = d; }
    public void setNom(String s){ this.nom = s; }
    public void setPrenom(String s){ this.prenom = s; }
    public void setCarte(String s){ this.carte = s; }
    public void setAgence(JAXBElement<String> e){ this.agence = e; }
}

