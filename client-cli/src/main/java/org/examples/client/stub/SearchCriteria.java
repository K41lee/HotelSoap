package org.examples.client.stub;

import javax.xml.datatype.XMLGregorianCalendar;

public class SearchCriteria {
    private String ville;
    private XMLGregorianCalendar arrivee;
    private XMLGregorianCalendar depart;
    private Integer prixMin, prixMax;
    private String categorie;
    private Integer nbEtoiles;
    private int nbPersonnes;
    private String agence;

    public void setVille(String v){ this.ville=v; }
    public void setArrivee(XMLGregorianCalendar a){ this.arrivee=a; }
    public void setDepart(XMLGregorianCalendar d){ this.depart=d; }
    public void setPrixMin(Integer p){ this.prixMin=p; }
    public void setPrixMax(Integer p){ this.prixMax=p; }
    public void setCategorie(String c){ this.categorie=c; }
    public void setNbEtoiles(Integer e){ this.nbEtoiles=e; }
    public void setNbPersonnes(int n){ this.nbPersonnes=n; }
    public void setAgence(String a){ this.agence=a; }
}

