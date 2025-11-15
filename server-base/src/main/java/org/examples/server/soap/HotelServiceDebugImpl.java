package org.examples.server.soap;

import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.examples.server.dto.*;
import Impl.AgencyCredentials;
import Impl.DataFactory;
import Impl.Gestionnaire;
import Impl.ReservationResult;
import Impl.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@WebService(endpointInterface = "org.examples.server.soap.HotelService", serviceName = "HotelServiceDebug")
@Component
@XmlSeeAlso({Offer.class, Address.class, Room.class, OfferList.class, Catalog.class, ReservationConfirmation.class, ReservationRequest.class, SearchCriteria.class, SearchOffersResponse.class})
public class HotelServiceDebugImpl implements HotelService {
    private static final Logger log = LoggerFactory.getLogger(HotelServiceDebugImpl.class);
    public static final String DEBUG_MARKER = "DEBUG_IMPL_ACTIVE";

    private final DataFactory factory = DataFactory.rivage();
    private Gestionnaire gestionnaire;

    public void setGestionnaire(Gestionnaire g){
        this.gestionnaire = g;
    }

    @Override
    public String ping() { return "pong-debug"; }

    @Override
    public SearchOffersResponse searchOffers(SearchCriteria criteria) {
        log.info("[DEBUG] {} searchOffers agence={} ville={} nbPers={}", DEBUG_MARKER, criteria.agence, criteria.ville, criteria.nbPersonnes);
        AgencyCredentials creds = new AgencyCredentials(criteria.agence, null, null);
        java.time.LocalDate from = criteria.arrivee != null ? criteria.arrivee.toGregorianCalendar().toZonedDateTime().toLocalDate() : java.time.LocalDate.now();
        java.time.LocalDate to = criteria.depart != null ? criteria.depart.toGregorianCalendar().toZonedDateTime().toLocalDate() : from.plusDays(1);
        java.util.List<org.examples.server.dto.Offer> dtoOffers = new java.util.ArrayList<>();
        for (Impl.Offer base : factory.findOffers(creds, from, to, criteria.nbPersonnes)) {
            org.examples.server.dto.Offer o = new org.examples.server.dto.Offer();
            o.offerId = base.getId();
            o.hotelName = factory.getHotelName();
            o.categorie = "DEBUG";
            o.nbEtoiles = 4;
            o.nbLits = base.getBeds();
            o.prixTotal = (int) base.getPrice();
            o.agenceApplied = creds.getAgencyId();
            o.roomNumber = 999;
            log.info("[DEBUG-MAP] id={} hotelName={} price={}", o.offerId, o.hotelName, o.prixTotal);
            dtoOffers.add(o);
        }
        OfferList list = new OfferList();
        list.setOffers(dtoOffers);
        SearchOffersResponse resp = new SearchOffersResponse();
        resp.setOffers(list);
        log.info("[DEBUG-RESP] totalOffers={}", dtoOffers.size());
        return resp;
    }

    @Override
    public ReservationConfirmation makeReservation(ReservationRequest request) throws ServiceFault {
        ReservationConfirmation rc = new ReservationConfirmation();
        rc.setSuccess(true);
        rc.setMessage("DEBUG reservation OK");
        rc.setReference("DEBUG-"+request.offerId);
        return rc;
    }

    @Override
    public Catalog getCatalog() {
        Catalog c = new Catalog();
        c.setName(factory.getHotelName()+"-DEBUG");
        return c;
    }
}
