package org.examples.client;

import org.examples.client.stub.*;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ClientMain {

    private static XMLGregorianCalendar toXgc(LocalDate d) throws Exception {
        return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
                d.getYear(), d.getMonthValue(), d.getDayOfMonth(),
                DatatypeConstants.FIELD_UNDEFINED
        );
    }

    public static void main(String[] args) throws Exception {
        String wsdlUrl = System.getProperty("wsdl.url", "http://localhost:8081/hotel-rivage/hotel?wsdl");
        System.out.println("WSDL : " + wsdlUrl);

        HotelService_Service svc = new HotelService_Service(new URL(wsdlUrl));
        HotelService port = svc.getHotelServiceImplPort();
        Scanner in = new Scanner(System.in);

        CatalogDTO cat = port.getCatalog();
        System.out.println("=== CATALOGUE ===");
        System.out.println("Nom hôtel: " + (cat.getName() != null ? cat.getName() : "(inconnu)"));
        // Affichage simple des villes/agences si exposées par les stubs
        try {
            CitiesDTO cities = cat.getCities();
            if (cities != null && cities.getCity() != null && !cities.getCity().isEmpty()) {
                System.out.println("Villes: " + cities.getCity());
            }
        } catch (Throwable ignore) {}
        try {
            AgenciesDTO agencies = cat.getAgencies();
            if (agencies != null && agencies.getAgency() != null && !agencies.getAgency().isEmpty()) {
                System.out.println("Agences: " + agencies.getAgency());
            }
        } catch (Throwable ignore) {}

        System.out.println("=== CLIENT SOAP ===");

        String ville;
        while (true) {
            System.out.print("Ville (ex: Sète/Montpellier) : ");
            ville = in.nextLine().trim();
            if (!ville.isEmpty()) break;
            System.out.println("Requis.");
        }

        LocalDate dArr;
        while (true) {
            System.out.print("Arrivée (YYYY-MM-DD) : ");
            String s = in.nextLine().trim();
            try { dArr = LocalDate.parse(s); break; }
            catch (DateTimeParseException e) { System.out.println("Format attendu YYYY-MM-DD."); }
        }
        LocalDate dDep;
        while (true) {
            System.out.print("Départ  (YYYY-MM-DD) : ");
            String s = in.nextLine().trim();
            try { dDep = LocalDate.parse(s); if (!dDep.isAfter(dArr)) { System.out.println("Départ doit être après l'arrivée."); continue; } break; }
            catch (DateTimeParseException e) { System.out.println("Format attendu YYYY-MM-DD."); }
        }
        int nbPers;
        while (true) {
            System.out.print("Nb personnes : ");
            String s = in.nextLine().trim();
            try { nbPers = Integer.parseInt(s); if (nbPers < 1) { System.out.println(">=1"); continue; } break; }
            catch (NumberFormatException e) { System.out.println("Entier attendu."); }
        }
        System.out.print("Agence (vide=aucune) : ");
        String agence = in.nextLine().trim();
        if (agence.isEmpty()) agence = null;

        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setVille(ville);
        criteria.setArrivee(toXgc(dArr));
        criteria.setDepart(toXgc(dDep));
        criteria.setNbPersonnes(nbPers);
        criteria.setAgence(agence);

        SearchOffersResponseDTO sr = port.searchOffers(criteria);
        OfferListDTO list = (sr != null) ? sr.getOffers() : null;
        List<OfferDTO> offers = (list != null) ? list.getOffers() : Collections.emptyList();
        if (offers.isEmpty()) {
            System.out.println("Aucune offre.");
            return;
        }
        System.out.println("\nOffres :");
        for (int i = 0; i < offers.size(); i++) {
            OfferDTO o = offers.get(i);
            String name = (o.getHotelName()!=null)? o.getHotelName() : "(inconnu)";
            String catStr = (o.getCategorie()!=null)? o.getCategorie() : "(n/c)";
            int stars = o.getNbEtoiles();
            int price = o.getPrixTotal();
            AddressDTO a = o.getAddress();
            String addr = (a!=null)? (String.format("%s %s, %s (%s)",
                    a.getNumero(),
                    a.getRue()!=null? a.getRue() : "",
                    a.getVille()!=null? a.getVille() : "",
                    a.getPays()!=null? a.getPays() : "")) : "(adresse n/c)";
            System.out.printf("%d) %s | %d★ %s | %d € | %s%n", i+1, name, stars, catStr, price, addr);
        }
        int idx;
        while (true) {
            System.out.print("\nChoisissez une offre [1-" + offers.size() + "] : ");
            String s = in.nextLine().trim();
            try { idx = Integer.parseInt(s) - 1; if (idx < 0 || idx >= offers.size()) { System.out.println("Indice invalide."); continue; } break; }
            catch (NumberFormatException e) { System.out.println("Entier attendu."); }
        }
        OfferDTO chosen = offers.get(idx);

        System.out.println("\n=== Réservation ===");
        String nom;
        while (true) { System.out.print("Nom : "); nom = in.nextLine().trim(); if (!nom.isEmpty()) break; System.out.println("Requis."); }
        String prenom;
        while (true) { System.out.print("Prénom : "); prenom = in.nextLine().trim(); if (!prenom.isEmpty()) break; System.out.println("Requis."); }
        String carte;
        while (true) { System.out.print("Carte (16 chiffres) : "); carte = in.nextLine().trim(); String n = carte.replaceAll("[ -]", ""); if (n.matches("\\d{16}")) break; System.out.println("Invalide."); }

        ReservationRequestDTO rq = new ReservationRequestDTO();
        // transmettre l'identifiant d'offre si présent pour un WS2 robuste
        if (chosen.getOfferId()!=null) rq.setOfferId(chosen.getOfferId());
        rq.setHotelName(chosen.getHotelName());
        rq.setRoomNumber((chosen.getRoom()!=null)? chosen.getRoom().getNumero() : 0);
        rq.setArrivee(toXgc(dArr));
        rq.setDepart(toXgc(dDep));
        rq.setNom(nom);
        rq.setPrenom(prenom);
        rq.setCarte(carte);
        if (agence != null) {
            ObjectFactory of = new ObjectFactory();
            JAXBElement<String> agenceEl = of.createReservationRequestDTOAgence(agence);
            rq.setAgence(agenceEl);
        }

        try {
            ReservationConfirmationDTO conf = port.makeReservation(rq);
            System.out.println("\n" + (conf.getMessage()!=null? conf.getMessage() : "(sans message)") + " — id=" + conf.getId());
            OfferDTO booked = conf.getOffer();
            if (booked != null) {
                String bname = booked.getHotelName()!=null? booked.getHotelName() : "(inconnu)";
                int bnum = (booked.getRoom()!=null)? booked.getRoom().getNumero() : 0;
                int bprice = booked.getPrixTotal();
                System.out.printf("Réservé: %s, ch.%d, total=%d €%n", bname, bnum, bprice);
            }
        } catch (ServiceFault_Exception sf) {
            System.out.println("Échec réservation : " + sf.getMessage());
        }
    }
}
