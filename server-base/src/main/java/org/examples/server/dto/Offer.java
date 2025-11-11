package org.examples.server.dto;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="OfferDTO", namespace="http://service.hotel.examples.org/dto")
@XmlRootElement(name="OfferDTO", namespace="http://service.hotel.examples.org/dto")
public class Offer {
    // déjà existants
    public String hotelName;
    public Address address;
    public String categorie;
    public int nbEtoiles;
    public Room room;
    public int prixTotal;          // prix déjà remisé pour l’agence
    public String agenceApplied;   // nom de l’agence appliquée ou null

    // AJOUTS pour 2.2
    public String offerId;                       // identifiant opaque renvoyé par WS1 et attendu par WS2
    @XmlSchemaType(name="date") public XMLGregorianCalendar start;  // période de validité de l’offre
    @XmlSchemaType(name="date") public XMLGregorianCalendar end;
    public int roomNumber;                       // redondant (utile côté client)
    public int nbLits;                           // redondant (utile côté client)

    public String getOfferId(){ return offerId; }
    public void setOfferId(String id){ this.offerId = id; }
    public String getHotelName(){ return hotelName; }
    public void setHotelName(String s){ this.hotelName = s; }
    public Address getAddress(){ return address; }
    public void setAddress(Address a){ this.address = a; }
    public String getCategorie(){ return categorie; }
    public void setCategorie(String c){ this.categorie = c; }
    public int getNbEtoiles(){ return nbEtoiles; }
    public void setNbEtoiles(int n){ this.nbEtoiles = n; }
    public Room getRoom(){ return room; }
    public void setRoom(Room r){ this.room = r; }
    public int getPrixTotal(){ return prixTotal; }
    public void setPrixTotal(int p){ this.prixTotal = p; }
    public String getAgenceApplied(){ return agenceApplied; }
    public void setAgenceApplied(String a){ this.agenceApplied = a; }
}
