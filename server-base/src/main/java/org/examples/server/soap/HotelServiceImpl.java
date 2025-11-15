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

    @Override
    public SearchOffersResponse searchOffers(SearchCriteria criteria) {
        logger.info("[MARKER] {} entering searchOffers", LOG_MARKER);
        logger.info("[REQ] searchOffers received: agence='{}' from='{}' to='{}' persons={} ville='{}' prixMin={} prixMax={} cat='{}' stars={}", criteria.agence, criteria.arrivee, criteria.depart, criteria.nbPersonnes, criteria.ville, criteria.prixMin, criteria.prixMax, criteria.categorie, criteria.nbEtoiles);
        AgencyCredentials creds = new AgencyCredentials(criteria.agence, null, null);
        java.time.LocalDate from = criteria.arrivee != null ? criteria.arrivee.toGregorianCalendar().toZonedDateTime().toLocalDate() : java.time.LocalDate.now();
        java.time.LocalDate to = criteria.depart != null ? criteria.depart.toGregorianCalendar().toZonedDateTime().toLocalDate() : from.plusDays(1);
        java.util.List<Impl.Offer> offers = factory.findOffers(creds, from, to, criteria.nbPersonnes);
        logger.info("[REQ] factory.findOffers -> {} offres", (offers!=null? offers.size(): -1));
        if (offers == null) offers = new java.util.ArrayList<>();

        // 1) Filtrage par ville via Gestionnaire
        java.util.List<Hotel> allHotels = (gestionnaire!=null)? gestionnaire.getHotels() : java.util.Collections.emptyList();
        String villeReq = normalize(criteria.ville);
        java.util.List<Hotel> eligibleHotels;
        if (villeReq != null && !villeReq.isEmpty()) {
            eligibleHotels = new java.util.ArrayList<>();
            for (Hotel h : allHotels) {
                String hv = (h.getAdresse()!=null && h.getAdresse().getVille()!=null)? normalize(h.getAdresse().getVille()) : null;
                if (hv!=null && hv.equals(villeReq)) eligibleHotels.add(h);
            }
            logger.info("[FILTER] hotels matching city='{}': {}", criteria.ville, eligibleHotels.size());
        } else {
            eligibleHotels = new java.util.ArrayList<>(allHotels);
        }
        if ((villeReq!=null && !villeReq.isEmpty()) && eligibleHotels.isEmpty()) {
            logger.info("[FILTER] No hotel matches city='{}' — returning 0 offers", criteria.ville);
            return emptyOffers();
        }

        // 2) Filtrage des offres par prix si demandé (sur prix agencé retourné par factory)
        java.util.List<Impl.Offer> filtered = new java.util.ArrayList<>();
        for (Impl.Offer o : offers) {
            int price = (int) Math.round(o.getPrice());
            if (criteria.prixMin != null && price < criteria.prixMin) continue;
            if (criteria.prixMax != null && price > criteria.prixMax) continue;
            filtered.add(o);
        }
        logger.info("[FILTER] after price range -> {} offers", filtered.size());

        // 3) Filtrage par nb étoiles / catégorie basé sur l'hôtel choisi pour le mapping
        //    On ne peut pas lier Impl.Offer à un hôtel précis — on associe chaque offre à un hôtel eligible
        //    en priorité le premier éligible.
        Hotel hotelForMapping = !eligibleHotels.isEmpty()? eligibleHotels.get(0) : (!allHotels.isEmpty()? allHotels.get(0) : null);
        if (hotelForMapping == null && (villeReq!=null && !villeReq.isEmpty())) {
            return emptyOffers();
        }
        if (hotelForMapping != null) {
            if (criteria.nbEtoiles != null && hotelForMapping.getNbEtoiles() != criteria.nbEtoiles.intValue()) {
                logger.info("[FILTER] hotel stars={} != requested {} -> returning 0 offers", hotelForMapping.getNbEtoiles(), criteria.nbEtoiles);
                return emptyOffers();
            }
            if (criteria.categorie != null && !criteria.categorie.trim().isEmpty()) {
                String hc = String.valueOf(hotelForMapping.getCategorie());
                if (!hc.equalsIgnoreCase(criteria.categorie.trim())) {
                    logger.info("[FILTER] hotel category='{}' != requested '{}' -> returning 0 offers", hc, criteria.categorie);
                    return emptyOffers();
                }
            }
        }

        // Si aucune offre mais ville non spécifiée, autoriser une offre synthétique pour diagnostic
        if (filtered.isEmpty()) {
            if (villeReq==null || villeReq.isEmpty()) {
                int persons = (criteria.nbPersonnes > 0) ? criteria.nbPersonnes : 2;
                filtered.add(new Impl.Offer(java.util.UUID.randomUUID().toString(), persons, from, to, 100.0));
                logger.info("[REQ] ajout d'une offre synthétique pour diagnostic (ville non spécifiée)");
            } else {
                logger.info("[FILTER] No offer from factory after filters and city specified -> return 0 offers");
                return emptyOffers();
            }
        }

        OfferList dtoList = new OfferList();
        java.util.List<org.examples.server.dto.Offer> mapped = new java.util.ArrayList<>();
        for (Impl.Offer o : filtered) {
            org.examples.server.dto.Offer of = new org.examples.server.dto.Offer();
            of.offerId = o.getId();
            of.nbLits = o.getBeds();
            Hotel h = hotelForMapping;
            if (h != null) {
                of.hotelName = h.getNom();
                of.categorie = String.valueOf(h.getCategorie());
                of.nbEtoiles = h.getNbEtoiles();
                Impl.Chambre chosen = h.getChambres().stream().filter(c -> c.getNbLits() == o.getBeds()).findFirst().orElse(null);
                if (chosen == null && !h.getChambres().isEmpty()) chosen = h.getChambres().get(0);
                if (chosen != null) {
                    of.roomNumber = chosen.getNumero();
                    org.examples.server.dto.Room r = new org.examples.server.dto.Room();
                    r.numero = chosen.getNumero();
                    r.nbLits = chosen.getNbLits();
                    of.room = r;
                }
                if (h.getAdresse() != null) {
                    org.examples.server.dto.Address addr = new org.examples.server.dto.Address();
                    addr.ville = h.getAdresse().getVille();
                    addr.pays = h.getAdresse().getPays();
                    addr.rue = h.getAdresse().getRue();
                    addr.numero = h.getAdresse().getNumero();
                    of.address = addr;
                }
            } else {
                // fallback ultra-dégradé
                of.hotelName = factory != null ? factory.getHotelName() : "Hotel-Fallback";
                of.categorie = "INCONNUE";
                of.nbEtoiles = 0;
            }
            try {
                of.start = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(o.getFrom().toString());
                of.end = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(o.getTo().toString());
            } catch (Exception e) { }
            of.prixTotal = (int) Math.round(o.getPrice());
            of.agenceApplied = creds.getAgencyId();
            mapped.add(of);
            logger.info("[MAP] offerId={} hotelName={} cat={} stars={} price={}", of.offerId, of.hotelName, of.categorie, of.nbEtoiles, of.prixTotal);
        }
        dtoList.setOffers(mapped);
        SearchOffersResponse resp = new SearchOffersResponse();
        resp.setOffers(dtoList);
        logger.info("[RESP] searchOffers returning {} offers (firstHotelName={})", mapped.size(), mapped.isEmpty()?null:mapped.get(0).hotelName);
        logger.info("[MARKER] {} leaving searchOffers", LOG_MARKER);
        return resp;
    }

    private SearchOffersResponse emptyOffers() {
        SearchOffersResponse resp = new SearchOffersResponse();
        resp.setOffers(new OfferList());
        return resp;
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.trim().toLowerCase();
    }

    @Override
    public ReservationConfirmation makeReservation(ReservationRequest request) throws ServiceFault {
        logger.info("[REQ] makeReservation received: agence='{}' offerId='{}' nom='{}' prenom='{}'", request.agence, request.offerId, request.nom, request.prenom);
        if (request.offerId == null || request.nom == null || request.prenom == null || request.carte == null) {
            ReservationConfirmation rc = new ReservationConfirmation();
            rc.setSuccess(false);
            rc.setMessage("Donnes manquantes");
            rc.setReference(null);
            return rc;
        }
        Impl.Client c = new Impl.Client(request.nom, request.prenom, request.carte);
        String agencyId = (request.auth != null) ? request.auth.agencyId : request.agence;
        String login = (request.auth != null) ? request.auth.login : null;
        String password = (request.auth != null) ? request.auth.password : null;
        Impl.ReservationResult res = factory.reserve(agencyId, login, password, request.offerId, c);
        ReservationConfirmation rc = new ReservationConfirmation();
        rc.setSuccess(res.isSuccess());
        rc.setMessage(res.getMessage());
        rc.setReference(res.getReference());
        return rc;
    }

    @Override
    public Catalog getCatalog() {
        logger.info("[REQ] getCatalog received");
        Catalog cat = new Catalog();
        if (gestionnaire != null) {
            // Renseigner un nom d'hôtel/catalogue pour l'affichage client
            if (!gestionnaire.getHotels().isEmpty()) {
                Hotel h0 = gestionnaire.getHotels().get(0);
                if (h0 != null && h0.getNom() != null) {
                    cat.setName(h0.getNom());
                }
            }
            java.util.Set<String> citySet = gestionnaire.getHotels().stream()
                    .map(Hotel::getAdresse)
                    .filter(a -> a != null && a.getVille() != null && !a.getVille().isEmpty())
                    .map(a -> a.getVille())
                    .collect(java.util.stream.Collectors.toSet());
            java.util.Set<String> agencySet = gestionnaire.getHotels().stream()
                    .flatMap(h -> h.getAgences().stream())
                    .map(Agence::getNom)
                    .filter(n -> n != null && !n.isEmpty())
                    .collect(java.util.stream.Collectors.toSet());
            Catalog.Cities cwrap = cat.getCities();
            cwrap.getCity().addAll(new java.util.ArrayList<>(citySet));
            Catalog.Agencies awrap = cat.getAgencies();
            awrap.getAgency().addAll(new java.util.ArrayList<>(agencySet));
            logger.info("[RESP] getCatalog cities={} agencies={} (from gestionnaire)", cwrap.getCity().size(), awrap.getAgency().size());
            return cat;
        }
        Catalog.Cities cwrap = cat.getCities();
        cwrap.getCity().add("FallbackCity");
        Catalog.Agencies awrap = cat.getAgencies();
        awrap.getAgency().add("fallbackAgency");
        cat.setName("Hotel-Catalog");
        logger.info("[RESP] getCatalog fallback cities=1 agencies=1");
        return cat;
    }
}
