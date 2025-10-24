// src/main/java/org/examples/server/dto/SearchCriteria.java
package org.examples.server.dto;

import jakarta.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;

@XmlRootElement(name="SearchCriteria")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchCriteria {
    public String ville;
    @XmlSchemaType(name="date") public XMLGregorianCalendar arrivee;
    @XmlSchemaType(name="date") public XMLGregorianCalendar depart;
    public Integer prixMin, prixMax;
    public String categorie;   // "ECONOMIQUE", etc.
    public Integer nbEtoiles;
    public int nbPersonnes;
    public String agence;      // optionnel
}
