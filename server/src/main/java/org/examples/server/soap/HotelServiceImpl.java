package org.examples.server.soap;

import Impl.*;
import org.examples.server.dto.*;
import org.examples.server.mapper.DomainMapper;

import jakarta.jws.WebService;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.*;

@Component
@WebService(endpointInterface="org.examples.server.soap.HotelService",
        targetNamespace="http://service.hotel.examples.org/")
public class HotelServiceImpl implements HotelService {

    private final Gestionnaire g;
    public HotelServiceImpl(Gestionnaire g){ this.g = g; }

    @Override
    public SearchOffersResponse searchOffers(SearchCriteria c) {
        LocalDate dArr = toLocalDate(c.arrivee);
        LocalDate dDep = toLocalDate(c.depart);

        // Catégorie optionnelle (compatible Java 8)
        Categorie cat = null;
        if (c.categorie != null && !c.categorie.trim().isEmpty()) {
            cat = Categorie.valueOf(c.categorie.trim().toUpperCase());
        }

        List<Gestionnaire.Offre> offres = g.findMatchReservation(
                c.ville,
                dArr, dDep,
                c.prixMin, c.prixMax,
                cat, c.nbEtoiles,
                c.nbPersonnes,
                (c.agence != null && !c.agence.trim().isEmpty()) ? c.agence.trim() : null
        );

        List<org.examples.server.dto.Offer> dtoList = offres.stream()
                .map(o -> {
                    String applied = (c.agence != null && !c.agence.trim().isEmpty()
                            && o.hotel().findAgenceByName(c.agence.trim()).isPresent())
                            ? c.agence.trim() : null;
                    return DomainMapper.toDto(o, applied);
                })
                .collect(java.util.stream.Collectors.toList());

        SearchOffersResponse resp = new SearchOffersResponse();
        resp.offers = dtoList;
        return resp;
    }

    @Override
    public Catalog getCatalog() {
        Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> agencies = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (Hotel h : g.getHotels()) {
            if (h.getAdresse() != null && h.getAdresse().getVille() != null) {
                cities.add(h.getAdresse().getVille());
            }
            // si tu as une liste d'agences sur l'hôtel :
            for (Agence a : h.getAgences()) { // adapte si le getter diffère
                if (a != null && a.getNom() != null) agencies.add(a.getNom());
            }
        }
        Catalog c = new Catalog();
        c.cities.addAll(cities);
        c.agencies.addAll(agencies);
        return c;
    }


    @Override
    public ReservationConfirmation makeReservation(ReservationRequest r) throws ServiceFault {
        // retrouver l'hôtel + chambre
        Hotel hotel = g.getHotels().stream()
                .filter(h -> h.getNom().equalsIgnoreCase(r.hotelName))
                .findFirst()
                .orElseThrow(() -> new ServiceFault("Hôtel introuvable: " + r.hotelName));

        Chambre chambre = hotel.getChambres().stream()
                .filter(c -> c.getNumero() == r.roomNumber)
                .findFirst()
                .orElseThrow(() -> new ServiceFault("Chambre introuvable: " + r.roomNumber));

        LocalDate dArr = toLocalDate(r.arrivee);
        LocalDate dDep = toLocalDate(r.depart);

        if (!chambre.isDisponible(dArr, dDep)) {
            throw new ServiceFault("Période indisponible");
        }

        Client client = new Client(r.nom, r.prenom, r.carte);
        Reservation resa = g.makeReservation(client, chambre, dArr, dDep);

        // --- Calcul du prix avec éventuelle réduction d’agence ---
        int prix = chambre.prixTotal(dArr, dDep);
        String applied = null;
        if (r.agence != null && !r.agence.trim().isEmpty()) {
            // si l’hôtel connaît cette agence -> appliquer la réduction
            java.util.Optional<Impl.Agence> opt = hotel.findAgenceByName(r.agence);
            if (opt.isPresent()) {
                double reduc = opt.get().getReduction(); // ex: 0.10 pour -10%
                prix = (int) Math.round(prix * (1.0 - reduc));
                applied = r.agence;
            }
        }

        // construire la réponse
        Gestionnaire.Offre off = new Gestionnaire.Offre(hotel, chambre, prix);

        ReservationConfirmation conf = new ReservationConfirmation();
        conf.id = java.util.UUID.randomUUID().toString();
        conf.message = "Réservation confirmée";
        conf.offer = DomainMapper.toDto(off, applied);
        return conf;
    }


    // helpers dates
    private static LocalDate toLocalDate(XMLGregorianCalendar xgc){
        return xgc.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }
}
