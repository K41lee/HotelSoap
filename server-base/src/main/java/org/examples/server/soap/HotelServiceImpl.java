package org.examples.server.soap;

import javax.jws.WebService;
import org.examples.server.dto.*;
import org.examples.server.soap.ServiceFault;
import Impl.DataFactory;
import Impl.AgencyCredentials;
import Impl.Client;
import Impl.ReservationResult;
import Impl.Gestionnaire;
import Impl.Hotel;
import Impl.Agence;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.text.Normalizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.ArrayList;

@WebService(endpointInterface = "org.examples.server.soap.HotelService", serviceName = "HotelService")
@Component
@XmlSeeAlso({Offer.class, Address.class, Room.class, OfferList.class, Catalog.class, ReservationConfirmation.class, ReservationRequest.class, SearchCriteria.class, SearchOffersResponse.class})
public class HotelServiceImpl implements HotelService {

    public static final String LOG_MARKER = "UNIQUE_MARKER_XYZ";
    private static final Logger logger = LoggerFactory.getLogger(HotelServiceImpl.class);

    // rendu non-final pour injection via setter
    private DataFactory factory = DataFactory.rivage();

    @Autowired(required = false)
    private Gestionnaire gestionnaire; // may be null if not provided by the server module

    public void setDataFactory(DataFactory factory) { this.factory = factory; }

    // Permet l'injection programmatique par Publisher si nécessaire
    public void setGestionnaire(Gestionnaire gestionnaire) { this.gestionnaire = gestionnaire; }

    @PostConstruct
    public void postConstruct() {
        logger.info("[INIT] HotelServiceImpl starting. DataFactory hotelName='{}'", factory != null ? factory.getHotelName() : "<null>");
        if (gestionnaire != null) {
            logger.info("[INIT] Gestionnaire injected with {} hotels", gestionnaire.getHotels().size());
            for (Hotel h : gestionnaire.getHotels()) {
                logger.info("[INIT] Hotel='{}' city='{}' category='{}' rooms={}", h.getNom(), h.getAdresse() != null ? h.getAdresse().getVille() : "<no-city>", h.getCategorie(), h.getChambres().size());
                if (!h.getAgences().isEmpty()) {
                    for (Agence a : h.getAgences()) {
                        logger.info("[INIT]   Agency='{}' reduction={}", a.getNom(), a.getReduction());
                    }
                }
            }
        } else {
            logger.info("[INIT] No Gestionnaire bean injected; using DataFactory fallback");
        }
    }

    @Override
    public String ping() { return "pong"; }

    // Suivi local des réservations effectuées (par hôtel normalisé -> liste de périodes)
    private final Map<String, java.util.List<ReservationPeriod>> reservationsByHotel = new ConcurrentHashMap<>();

    private static class ReservationPeriod {
        final int roomNumber; final LocalDate from; final LocalDate to;
        ReservationPeriod(int roomNumber, LocalDate from, LocalDate to) { this.roomNumber = roomNumber; this.from = from; this.to = to; }
        boolean overlaps(LocalDate f, LocalDate t) { return (from.isBefore(t) && f.isBefore(to)); }
    }

    private void registerReservation(String hotelName, int roomNumber, LocalDate from, LocalDate to) {
        String key = normalize(hotelName);
        reservationsByHotel.computeIfAbsent(key, k -> new ArrayList<>()).add(new ReservationPeriod(roomNumber, from, to));
        logger.info("[TRACK] reservation stored hotel='{}' room={} from={} to={} totalStored={}"
                , hotelName, roomNumber, from, to, reservationsByHotel.get(key).size());
    }

    private boolean isRoomAvailableLocal(String hotelName, int roomNumber, LocalDate from, LocalDate to) {
        String key = normalize(hotelName);
        java.util.List<ReservationPeriod> list = reservationsByHotel.get(key);
        if (list == null) return true;
        for (ReservationPeriod rp : list) {
            if (rp.roomNumber == roomNumber && rp.overlaps(from, to)) {
                logger.info("[FILTER] room {} of '{}' excluded (overlap {}-{} with existing {}-{})", roomNumber, hotelName, from, to, rp.from, rp.to);
                return false;
            }
        }
        return true;
    }

    @Override
    public SearchOffersResponse searchOffers(SearchCriteria criteria) {
        logger.info("[REQ] searchOffers received agence='{}' ville='{}' arrivee='{}' depart='{}' persons={} prixMin={} prixMax={} cat='{}' stars={}",
                criteria.agence, criteria.ville, criteria.arrivee, criteria.depart, criteria.nbPersonnes,
                criteria.prixMin, criteria.prixMax, criteria.categorie, criteria.nbEtoiles);
        LocalDate from = criteria.arrivee != null ? criteria.arrivee.toGregorianCalendar().toZonedDateTime().toLocalDate() : LocalDate.now();
        LocalDate to = criteria.depart != null ? criteria.depart.toGregorianCalendar().toZonedDateTime().toLocalDate() : from.plusDays(1);
        if (!from.isBefore(to)) {
            logger.warn("[FILTER] date range invalid from >= to");
            return emptyOffers();
        }
        if (gestionnaire == null) {
            logger.warn("[WARN] gestionnaire null -> fallback DataFactory");
            return legacySearchOffersFallback(criteria, from, to);
        }
        Impl.Categorie catEnum = null;
        if (criteria.categorie != null && !criteria.categorie.trim().isEmpty()) {
            try { catEnum = Impl.Categorie.valueOf(criteria.categorie.trim()); } catch(Exception e){ logger.info("[FILTER] unknown categorie '{}'", criteria.categorie); }
        }
        // NB: prixMin/prixMax peuvent être null — utiliser directement
        List<Gestionnaire.Offre> matches = gestionnaire.findMatchReservation(
                safeString(criteria.ville), from, to,
                criteria.prixMin, criteria.prixMax,
                catEnum, criteria.nbEtoiles != null ? criteria.nbEtoiles : null,
                criteria.nbPersonnes,
                criteria.agence
        );
        logger.info("[GEST] returned {} raw offers", matches.size());
        OfferList list = new OfferList();
        List<org.examples.server.dto.Offer> dto = new ArrayList<>();
        for (Gestionnaire.Offre o : matches) {
            Impl.Chambre c = o.chambre(); Impl.Hotel h = o.hotel();
            // Filtre local (déjà réservée via cache)
            if (!isRoomAvailableLocal(h.getNom(), c.getNumero(), from, to)) {
                continue;
            }
            org.examples.server.dto.Offer of = new org.examples.server.dto.Offer();
            of.offerId = buildOfferId(h, c, from, to, criteria.nbPersonnes);
            of.hotelName = h.getNom();
            of.nbLits = c.getNbLits();
            of.nbEtoiles = h.getNbEtoiles();
            of.categorie = String.valueOf(h.getCategorie());
            of.roomNumber = c.getNumero();
            org.examples.server.dto.Room rr = new org.examples.server.dto.Room(); rr.numero = c.getNumero(); rr.nbLits = c.getNbLits(); of.room = rr;
            if (h.getAdresse()!=null) {
                org.examples.server.dto.Address a = new org.examples.server.dto.Address();
                a.ville = h.getAdresse().getVille(); a.pays = h.getAdresse().getPays(); a.rue = h.getAdresse().getRue(); a.numero = h.getAdresse().getNumero();
                of.address = a;
            }
            try { of.start = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(from.toString()); of.end = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(to.toString()); } catch(Exception ignored) {}
            of.prixTotal = o.prixTotal();
            of.agenceApplied = criteria.agence;
            dto.add(of);
            logger.info("[MAP] offerId={} hotel='{}' room={} lits={} price={} city='{}'", of.offerId, of.hotelName, of.roomNumber, of.nbLits, of.prixTotal, of.address!=null? of.address.ville: "?");
        }
        list.setOffers(dto);
        SearchOffersResponse resp = new SearchOffersResponse(); resp.setOffers(list);
        logger.info("[RESP] searchOffers returning {} offers", dto.size());
        return resp;
    }

    private SearchOffersResponse legacySearchOffersFallback(SearchCriteria criteria, LocalDate from, LocalDate to) {
        AgencyCredentials creds = new AgencyCredentials(criteria.agence, null, null);
        List<Impl.Offer> offers = factory.findOffers(creds, from, to, criteria.nbPersonnes);
        OfferList dtoList = new OfferList();
        List<org.examples.server.dto.Offer> mapped = new java.util.ArrayList<>();
        for (Impl.Offer o : offers) {
            org.examples.server.dto.Offer of = new org.examples.server.dto.Offer();
            of.offerId = o.getId();
            of.hotelName = factory.getHotelName();
            of.nbLits = o.getBeds();
            of.nbEtoiles = 0;
            of.categorie = "INCONNUE";
            try {
                of.start = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(o.getFrom().toString());
                of.end = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(o.getTo().toString());
            } catch (Exception ignored) {}
            of.prixTotal = (int)Math.round(o.getPrice());
            of.agenceApplied = creds.getAgencyId();
            mapped.add(of);
        }
        dtoList.setOffers(mapped);
        SearchOffersResponse resp = new SearchOffersResponse();
        resp.setOffers(dtoList);
        return resp;
    }

    private static String buildOfferId(Impl.Hotel h, Impl.Chambre c, LocalDate from, LocalDate to, int persons) {
        return "OF|" + normalize(h.getNom()) + "|" + c.getNumero() + "|" + from + "|" + to + "|" + persons;
    }

    private static class ParsedOfferId {
        String hotelKey; int roomNumber; LocalDate from; LocalDate to; int persons;
    }
    private ParsedOfferId parseOfferId(String offerId) {
        if (offerId == null || !offerId.startsWith("OF|")) return null;
        String[] parts = offerId.split("\\|");
        if (parts.length != 6) return null;
        try {
            ParsedOfferId p = new ParsedOfferId();
            p.hotelKey = parts[1];
            p.roomNumber = Integer.parseInt(parts[2]);
            p.from = LocalDate.parse(parts[3]);
            p.to = LocalDate.parse(parts[4]);
            p.persons = Integer.parseInt(parts[5]);
            return p;
        } catch (Exception e) { return null; }
    }

    @Override
    public ReservationConfirmation makeReservation(ReservationRequest request) throws ServiceFault {
        logger.info("[REQ] makeReservation received agence='{}' offerId='{}' hotel='{}' room={} nom='{}' prenom='{}'", request.agence, request.offerId, request.hotelName, request.roomNumber, request.nom, request.prenom);
        ReservationConfirmation rc = new ReservationConfirmation();
        if (gestionnaire == null) {
            logger.warn("[WARN] gestionnaire null -> legacy reservation");
            return legacyReservation(request);
        }
        // Priorité: offerId
        ParsedOfferId parsed = request.offerId != null && !request.offerId.trim().isEmpty() ? parseOfferId(request.offerId) : null;
        Impl.Hotel hotel = null; Impl.Chambre chambre = null; LocalDate from = null; LocalDate to = null;
        if (parsed != null) {
            hotel = gestionnaire.getHotels().stream().filter(h -> normalize(h.getNom()).equals(parsed.hotelKey)).findFirst().orElse(null);
            if (hotel != null) chambre = hotel.getChambres().stream().filter(c -> c.getNumero() == parsed.roomNumber).findFirst().orElse(null);
            from = parsed != null ? parsed.from : null; to = parsed != null ? parsed.to : null;
        } else if (request.hotelName != null && request.roomNumber > 0 && request.arrivee != null && request.depart != null) {
            try {
                from = request.arrivee.toGregorianCalendar().toZonedDateTime().toLocalDate();
                to = request.depart.toGregorianCalendar().toZonedDateTime().toLocalDate();
                hotel = gestionnaire.getHotels().stream().filter(h -> normalize(h.getNom()).equals(normalize(request.hotelName))).findFirst().orElse(null);
                if (hotel != null) chambre = hotel.getChambres().stream().filter(c -> c.getNumero() == request.roomNumber).findFirst().orElse(null);
            } catch(Exception e){ logger.warn("[WARN] date parsing fail {}", e.toString()); }
        }
        if (hotel == null || chambre == null || from == null || to == null) {
            rc.setSuccess(false); rc.setMessage("Paramètres invalides"); return rc;
        }
        if (!from.isBefore(to)) { rc.setSuccess(false); rc.setMessage("Période invalide"); return rc; }
        if (!isRoomAvailableLocal(hotel.getNom(), chambre.getNumero(), from, to)) { rc.setSuccess(false); rc.setMessage("Déjà réservé (cache)"); return rc; }
        if (!chambre.isDisponible(from, to)) { rc.setSuccess(false); rc.setMessage("Chambre non disponible"); return rc; }
        Impl.Client client = new Impl.Client(request.nom, request.prenom, request.carte);
        try {
            gestionnaire.makeReservation(client, chambre, from, to);
            registerReservation(hotel.getNom(), chambre.getNumero(), from, to);
        } catch(Exception e){ rc.setSuccess(false); rc.setMessage("Erreur réservation: "+e.getMessage()); return rc; }
        String ref = hotel.getNom().substring(0, Math.min(4, hotel.getNom().length())).toUpperCase() + "-" + java.util.UUID.randomUUID();
        rc.setSuccess(true); rc.setMessage("Réservation confirmée"); rc.setReference(ref);
        logger.info("[RESP] reservation ok ref={} hotel='{}' room={} from={} to={} stored={} ", ref, hotel.getNom(), chambre.getNumero(), from, to, reservationsByHotel.get(normalize(hotel.getNom())).size());
        return rc;
    }

    private ReservationConfirmation legacyReservation(ReservationRequest request) throws ServiceFault {
        ReservationConfirmation rc = new ReservationConfirmation();
        if (request.offerId == null) { rc.setSuccess(false); rc.setMessage("offerId manquant"); return rc; }
        Impl.Client c = new Impl.Client(request.nom, request.prenom, request.carte);
        Impl.ReservationResult r = factory.reserve(request.agence, null, null, request.offerId, c);
        rc.setSuccess(r.isSuccess()); rc.setMessage(r.getMessage()); rc.setReference(r.getReference());
        return rc;
    }

    private static String safeString(String s) { return s == null ? "" : s.trim(); }

    private SearchOffersResponse emptyOffers() {
        SearchOffersResponse resp = new SearchOffersResponse();
        OfferList list = new OfferList();
        list.setOffers(new java.util.ArrayList<org.examples.server.dto.Offer>());
        resp.setOffers(list);
        return resp;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return n;
    }

    @Override
    public Catalog getCatalog() {
        Catalog c = new Catalog();
        if (gestionnaire != null && !gestionnaire.getHotels().isEmpty()) {
            Impl.Hotel h = gestionnaire.getHotels().get(0);
            c.setName(h.getNom());
            Catalog.Cities cities = new Catalog.Cities();
            cities.getCity().add(h.getAdresse() != null ? h.getAdresse().getVille() : "");
            c.setCities(cities);
            Catalog.Agencies ags = new Catalog.Agencies();
            for (Impl.Agence a : h.getAgences()) ags.getAgency().add(a.getNom());
            c.setAgencies(ags);
            logger.info("[RESP] getCatalog cities={} agencies={} (from gestionnaire)", cities.getCity().size(), ags.getAgency().size());
        } else {
            c.setName(factory.getHotelName());
            logger.info("[RESP] getCatalog (fallback) name={}", c.getName());
        }
        return c;
    }
}
