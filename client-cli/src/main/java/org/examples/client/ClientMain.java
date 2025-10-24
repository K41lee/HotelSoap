package org.examples.client;

import org.examples.client.stub.*;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.soap.SOAPFaultException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ClientMain {

    private static XMLGregorianCalendar toXgc(LocalDate d) throws Exception {
        // date-only (heure non définie)
        return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
                d.getYear(), d.getMonthValue(), d.getDayOfMonth(),
                DatatypeConstants.FIELD_UNDEFINED
        );
    }

    public static void main(String[] args) throws Exception {
        String wsdlUrl = System.getProperty("wsdl.url", "http://localhost:8080/hotelservice?wsdl");
        System.out.println("WSDL : " + wsdlUrl);

        HotelServiceImplService svc = new HotelServiceImplService(new URL(wsdlUrl));
        HotelService port = svc.getHotelServiceImplPort();
        Scanner in = new Scanner(System.in);

        // Récupère le catalogue et construit des listes + maps canoniques (insensible à la casse)
        Catalog cat = port.getCatalog();

        Catalog.Cities cwrap = cat.getCities();
        List<String> cities = (cwrap == null || cwrap.getCity() == null)
                ? Collections.emptyList()
                : new ArrayList<>(cwrap.getCity());

        Catalog.Agencies awrap = cat.getAgencies();
        List<String> agencies = (awrap == null || awrap.getAgency() == null)
                ? Collections.emptyList()
                : new ArrayList<>(awrap.getAgency());

        Map<String,String> cityCanon = new HashMap<>();
        for (String c : cities) cityCanon.put(c.toLowerCase(Locale.ROOT), c);
        Map<String,String> agencyCanon = new HashMap<>();
        for (String a : agencies) agencyCanon.put(a.toLowerCase(Locale.ROOT), a);

        Runnable showCatalog = () -> {
            System.out.println("\n=== Villes disponibles ===");
            System.out.println(cities.isEmpty() ? "(aucune)" : String.join(", ", cities));
            System.out.println("=== Agences partenaires ===");
            System.out.println(agencies.isEmpty() ? "(aucune)" : String.join(", ", agencies));
            System.out.println();
        };

        System.out.println("=== CLIENT SOAP ===");

        // Ville (obligatoire) avec '?' et contrôle d'existence
        String ville;
        while (true) {
            System.out.print("Ville (tapez '?' pour lister) : ");
            String s = in.nextLine().trim();
            if ("?".equals(s)) { showCatalog.run(); continue; }
            if (s.isEmpty()) { System.out.println("Champ requis."); continue; }
            String canon = cityCanon.get(s.toLowerCase(Locale.ROOT));
            if (canon == null) { System.out.println("Ville inconnue. Tapez '?' pour lister."); continue; }
            ville = canon;
            break;
        }

        // Dates (format ISO) + départ après arrivée
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
            try {
                dDep = LocalDate.parse(s);
                if (!dDep.isAfter(dArr)) { System.out.println("Départ doit être après l'arrivée."); continue; }
                break;
            } catch (DateTimeParseException e) { System.out.println("Format attendu YYYY-MM-DD."); }
        }

        // Prix min/max optionnels (>=0) avec cohérence
        Integer prixMin = null, prixMax = null;
        while (true) {
            System.out.print("Prix min (vide=ignore) : ");
            String s = in.nextLine().trim();
            if (!s.isEmpty()) {
                try { prixMin = Integer.parseInt(s); if (prixMin < 0) { System.out.println("Doit être >= 0."); prixMin=null; continue; } }
                catch (NumberFormatException e) { System.out.println("Entier attendu."); continue; }
            }
            System.out.print("Prix max (vide=ignore) : ");
            s = in.nextLine().trim();
            if (!s.isEmpty()) {
                try { prixMax = Integer.parseInt(s); if (prixMax < 0) { System.out.println("Doit être >= 0."); prixMin=null; prixMax=null; continue; } }
                catch (NumberFormatException e) { System.out.println("Entier attendu."); prixMin=null; prixMax=null; continue; }
            }
            if (prixMin != null && prixMax != null && prixMin > prixMax) {
                System.out.println("min>max → inversion automatique.");
                int tmp = prixMin; prixMin = prixMax; prixMax = tmp;
            }
            break;
        }

        // Catégorie optionnelle (liste indicative via '?')
        String catStr = null;
        while (true) {
            System.out.print("Catégorie (vide=ignore, '?' pour exemples) : ");
            String s = in.nextLine().trim();
            if ("?".equals(s)) {
                System.out.println("Exemples : ECONOMIQUE, MILIEU_DE_GAMME, HAUT_DE_GAMME, LUXE");
                continue;
            }
            if (s.isEmpty()) { catStr = null; break; }
            catStr = s.toUpperCase(Locale.ROOT);
            break;
        }

        // Étoiles optionnel (1..5)
        Integer nbEtoiles = null;
        while (true) {
            System.out.print("Étoiles exact (vide=ignore) : ");
            String s = in.nextLine().trim();
            if (s.isEmpty()) break;
            try {
                int v = Integer.parseInt(s);
                if (v < 1 || v > 5) { System.out.println("Doit être entre 1 et 5."); continue; }
                nbEtoiles = v; break;
            } catch (NumberFormatException e) { System.out.println("Entier attendu."); }
        }

        // Nb personnes (obligatoire, 1..20)
        int nbPers;
        while (true) {
            System.out.print("Nb personnes : ");
            String s = in.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v < 1 || v > 20) { System.out.println("1..20."); continue; }
                nbPers = v; break;
            } catch (NumberFormatException e) { System.out.println("Entier attendu."); }
        }

        // Agence optionnelle avec '?' et contrôle d'existence
        String agence = null;
        while (true) {
            System.out.print("Agence (vide=aucune, '?' pour lister) : ");
            String s = in.nextLine().trim();
            if ("?".equals(s)) { showCatalog.run(); continue; }
            if (s.isEmpty()) { agence = null; break; }
            String canon = agencyCanon.get(s.toLowerCase(Locale.ROOT));
            if (canon == null) { System.out.println("Agence inconnue. Tapez '?' pour lister."); continue; }
            agence = canon; break;
        }

        // Construire critères et appel de recherche
        SearchCriteria criteria = new SearchCriteria();
        criteria.setVille(ville);
        criteria.setArrivee(toXgc(dArr));
        criteria.setDepart(toXgc(dDep));
        criteria.setPrixMin(prixMin);
        criteria.setPrixMax(prixMax);
        criteria.setCategorie(catStr);
        criteria.setNbEtoiles(nbEtoiles);
        criteria.setNbPersonnes(nbPers);
        criteria.setAgence(agence);

        SearchOffersPayload resp = port.searchOffers(criteria);
        List<Offer> offers = (resp.getOffer() == null) ? Collections.emptyList() : resp.getOffer();

        if (offers.isEmpty()) {
            System.out.println("Aucune offre.");
            return;
        }

        System.out.println("\nOffres :");
        for (int i = 0; i < offers.size(); i++) {
            Offer o = offers.get(i);
            Address a = o.getAddress();
            Room r = o.getRoom();
            System.out.printf(
                    "%d) %s — %s, %s %d, %s | ch.%d (%d lits) | %d★, %s | %d €%s%n",
                    i + 1,
                    o.getHotelName(),
                    a.getVille(), a.getRue(), a.getNumero(), a.getPays(),
                    (r != null ? r.getNumero() : -1),
                    (r != null ? r.getNbLits() : 0),
                    o.getNbEtoiles(), o.getCategorie(),
                    o.getPrixTotal(),
                    (o.getAgenceApplied() != null ? " [agence: " + o.getAgenceApplied() + "]" : "")
            );
        }

        int idx;
        while (true) {
            System.out.print("\nChoisissez une offre [1-" + offers.size() + "] : ");
            String s = in.nextLine().trim();
            try {
                idx = Integer.parseInt(s) - 1;
                if (idx < 0 || idx >= offers.size()) { System.out.println("Indice invalide."); continue; }
                break;
            } catch (NumberFormatException e) { System.out.println("Entier attendu."); }
        }
        Offer chosen = offers.get(idx);

        System.out.println("\n=== Réservation ===");

        String nom;
        while (true) {
            System.out.print("Nom : ");
            nom = in.nextLine().trim();
            if (nom.isEmpty()) { System.out.println("Requis."); continue; }
            if (nom.length() > 64) { System.out.println("Trop long (max 64)."); continue; }
            break;
        }

        String prenom;
        while (true) {
            System.out.print("Prénom : ");
            prenom = in.nextLine().trim();
            if (prenom.isEmpty()) { System.out.println("Requis."); continue; }
            if (prenom.length() > 64) { System.out.println("Trop long (max 64)."); continue; }
            break;
        }

        String carte;
        while (true) {
            System.out.print("Carte (16 chiffres, espaces/tirets ok) : ");
            carte = in.nextLine().trim();
            String normalized = carte.replaceAll("[ -]", "");
            if (normalized.matches("\\d{16}")) break;
            System.out.println("Carte invalide.");
        }

        ReservationRequest rq = new ReservationRequest();
        rq.setHotelName(chosen.getHotelName());
        rq.setRoomNumber(chosen.getRoom().getNumero());
        rq.setArrivee(toXgc(dArr));
        rq.setDepart(toXgc(dDep));
        rq.setNom(nom);
        rq.setPrenom(prenom);
        rq.setCarte(carte);

        if (agence != null && !agence.trim().isEmpty()) {
            ObjectFactory of = new ObjectFactory();
            JAXBElement<String> agenceEl = of.createReservationRequestAgence(agence);
            rq.setAgence(agenceEl);
        }

        try {
            ReservationConfirmation conf = port.makeReservation(rq);
            System.out.println("\n✅ " + conf.getMessage() + " — id=" + conf.getId());
            Offer booked = conf.getOffer();
            System.out.printf("Hôtel %s, ch.%d, total=%d €%n",
                    booked.getHotelName(), booked.getRoom().getNumero(), booked.getPrixTotal());
        } catch (SOAPFaultException sf) {
            System.out.println("Échec réservation : " + sf.getFault().getFaultString());
        }
    }
}
