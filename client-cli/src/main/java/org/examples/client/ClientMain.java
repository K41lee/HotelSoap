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

        String useAgency = System.getProperty("agency.tcp.enabled", "true");
        if ("true".equalsIgnoreCase(useAgency)) {
            String ahost = System.getProperty("agency.tcp.host", "localhost");
            int aport = Integer.parseInt(System.getProperty("agency.tcp.port", "7070"));
            try (AgencyTcpClient agency = new AgencyTcpClient(ahost, aport)) {
                // Catalogue via agence
                String catJson = agency.getCatalog();
                String agencyName = MiniJson.getString(catJson, "name");
                System.out.println("=== CATALOGUE (via Agence) ===");
                System.out.println("Nom agence: " + (agencyName != null ? agencyName : "(inconnu)"));
                List<String> cityList = MiniJson.getStringArray(catJson, "cities");
                if (cityList != null && !cityList.isEmpty()) {
                    System.out.println("Villes disponibles:");
                    for (int i=0;i<cityList.size();i++) System.out.println("  " + (i+1) + ") " + cityList.get(i));
                } else {
                    System.out.println("(Aucune ville transmise par l'agence)");
                }
                List<String> agenciesList = MiniJson.getStringArray(catJson, "agencies");
                if (agenciesList != null && !agenciesList.isEmpty()) {
                    System.out.println("Agences partenaires: " + agenciesList);
                }

                System.out.println("=== CLIENT (via Agence) ===");

                String ville;
                while (true) {
                    if (cityList != null && !cityList.isEmpty()) {
                        System.out.print("Ville (numéro ou texte, '?' pour lister) : ");
                        String s = in.nextLine().trim();
                        if (s.equals("?")) {
                            for (int i=0;i<cityList.size();i++) System.out.println("  " + (i+1) + ") " + cityList.get(i));
                            continue;
                        }
                        // choix numérique
                        try {
                            int ix = Integer.parseInt(s) - 1;
                            if (ix >= 0 && ix < cityList.size()) { ville = cityList.get(ix); break; }
                        } catch (NumberFormatException ignore) {}
                        // tentative de match texte normalisé
                        if (!s.isEmpty()) {
                            String ns = normalizeCity(s);
                            String matched = null;
                            for (String c : cityList) {
                                if (normalizeCity(c).equals(ns)) { matched = c; break; }
                            }
                            if (matched == null) {
                                for (String c : cityList) { // startsWith tolérant
                                    if (normalizeCity(c).startsWith(ns)) { matched = c; break; }
                                }
                            }
                            if (matched != null) { ville = matched; break; }
                            System.out.println("Ville inconnue. Tapez '?' pour lister.");
                            continue;
                        }
                        System.out.println("Requis.");
                    } else {
                        System.out.print("Ville (ex: Sète/Montpellier) : ");
                        String s = in.nextLine().trim();
                        if (!s.isEmpty()) { ville = s; break; }
                        System.out.println("Requis.");
                    }
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
                // NE PLUS DEMANDER l'agence en mode agence; l’agence locale gère tout.
                String srJson = agency.search(ville, dArr.toString(), dDep.toString(), nbPers, "");
                if (srJson == null || srJson.isEmpty()) {
                    System.out.println("(Agence) aucune réponse, nouvelle tentative...");
                    srJson = agency.search(ville, dArr.toString(), dDep.toString(), nbPers, "");
                    if (srJson == null || srJson.isEmpty()) {
                        System.out.println("Agence indisponible pour la recherche. Réessayez plus tard.");
                        return;
                    }
                }
                List<String> lines = new ArrayList<>();
                List<String> offerIds = new ArrayList<>();
                List<String> hotelCodes = new ArrayList<>();
                parseOffersForDisplay(srJson, lines, offerIds, hotelCodes);
                if (lines.isEmpty()) {
                    // Informer l’utilisateur de la réponse vide
                    String arr = MiniJson.getArray(srJson, "offers");
                    if (arr == null) System.out.println("Agence: réponse reçue mais sans champ 'offers'.");
                    else System.out.println("Aucune offre renvoyée par l'agence.");
                    return;
                }
                System.out.println("\nOffres :");
                for (int i=0;i<lines.size();i++) System.out.println((i+1)+") "+lines.get(i));
                int idx;
                while (true) {
                    System.out.print("\nChoisissez une offre [1-" + lines.size() + "] : ");
                    String s = in.nextLine().trim();
                    try { idx = Integer.parseInt(s) - 1; if (idx < 0 || idx >= lines.size()) { System.out.println("Indice invalide."); continue; } break; }
                    catch (NumberFormatException e) { System.out.println("Entier attendu."); }
                }
                String chosenOfferId = offerIds.get(idx);
                String chosenHotelCode = hotelCodes.get(idx);

                System.out.println("\n=== Réservation ===");
                String nom;
                while (true) { System.out.print("Nom : "); nom = in.nextLine().trim(); if (!nom.isEmpty()) break; System.out.println("Requis."); }
                String prenom;
                while (true) { System.out.print("Prénom : "); prenom = in.nextLine().trim(); if (!prenom.isEmpty()) break; System.out.println("Requis."); }
                String carte;
                while (true) { System.out.print("Carte (16 chiffres) : "); carte = in.nextLine().trim(); String n = carte.replaceAll("[ -]", ""); if (n.matches("\\d{16}")) break; System.out.println("Invalide."); }

                // Passer agencyId vide côté agence
                String rj = agency.reserve(chosenHotelCode, chosenOfferId, "", nom, prenom, carte);
                System.out.println(rj);
                return;
            }
        }

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

    // helpers JSON très simples pour extraire ce qu'il faut
    static class MiniJson {
        static String getString(String json, String key) {
            String pat = "\""+key+"\":\""; int i=json.indexOf(pat); if (i<0) return null; i+=pat.length(); int j=json.indexOf('"', i); if (j<0) return null; return json.substring(i, j);
        }
        static Integer getInt(String json, String key) {
            String pat = "\""+key+"\":"; int i=json.indexOf(pat); if (i<0) return null; i+=pat.length(); int j=i; while (j<json.length() && "0123456789".indexOf(json.charAt(j))>=0) j++; try { return Integer.parseInt(json.substring(i,j)); } catch(Exception e){ return null; }
        }
        static String getArray(String json, String key) {
            String pat = "\""+key+"\":["; int i=json.indexOf(pat); if (i<0) return null; i+=pat.length(); int j=json.indexOf(']', i); if (j<0) return null; return json.substring(i, j);
        }
        static List<String> getStringArray(String json, String key) {
            String arr = getArray(json, key);
            if (arr == null) return Collections.emptyList();
            List<String> out = new ArrayList<>();
            int idx = 0;
            while (true) {
                int q1 = arr.indexOf('"', idx); if (q1<0) break; int q2 = arr.indexOf('"', q1+1); if (q2<0) break; out.add(arr.substring(q1+1, q2)); idx = q2+1;
            }
            return out;
        }
    }

    private static String normalizeCity(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        n = n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }

    static void parseOffersForDisplay(String json, List<String> lines, List<String> offerIds, List<String> hotelCodes) {
        if (json == null || json.isEmpty()) return;
        int idx = 0;
        while (true) {
            int hi = json.indexOf("\"hotelName\":\"", idx); if (hi<0) break; hi += "\"hotelName\":\"".length(); int hj = json.indexOf('"', hi); if (hj<0) break; String name = json.substring(hi, hj);
            Integer stars = MiniJson.getInt(json.substring(hj), "nbEtoiles"); if (stars==null) stars=0;
            String cat = MiniJson.getString(json.substring(hj), "categorie"); if (cat==null) cat="(n/c)";
            Integer price = MiniJson.getInt(json.substring(hj), "prixTotal"); if (price==null) price=0;
            String city = MiniJson.getString(json.substring(hj), "ville"); if (city==null) city="";
            String pays = MiniJson.getString(json.substring(hj), "pays"); if (pays==null) pays="";
            String rue = MiniJson.getString(json.substring(hj), "rue"); if (rue==null) rue="";
            Integer num = MiniJson.getInt(json.substring(hj), "numero"); if (num==null) num=0;
            String offerId = MiniJson.getString(json.substring(hj), "offerId");
            String hotelCode = MiniJson.getString(json.substring(hj), "hotelCode");
            String line = String.format("%s | %d★ %s | %d € | %s %s, %s (%s)", name, stars, cat, price, num, rue, city, pays);
            lines.add(line); offerIds.add(offerId!=null?offerId:""); hotelCodes.add(hotelCode!=null?hotelCode:"");
            idx = hj + 1;
        }
    }
}
