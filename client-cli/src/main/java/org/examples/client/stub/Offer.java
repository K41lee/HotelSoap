package org.examples.client.stub;

public class Offer {
    private String hotelName;
    private Address address;
    private String categorie;
    private int nbEtoiles;
    private Room room;
    private int prixTotal;
    private String agenceApplied;

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

