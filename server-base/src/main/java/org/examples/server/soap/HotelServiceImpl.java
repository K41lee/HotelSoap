package org.examples.server.soap;

import javax.jws.WebService;
import org.examples.server.dto.*;
import org.examples.server.soap.ServiceFault;
import Impl.DataFactory;
import Impl.AgencyCredentials;
import Impl.Client;
import Impl.ReservationResult;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@WebService(endpointInterface = "org.examples.server.soap.HotelService", serviceName = "HotelService")
@Component
public class HotelServiceImpl implements HotelService {

    // rendu non-final pour injection via setter
    private DataFactory factory = DataFactory.rivage();

    public void setDataFactory(DataFactory factory) { this.factory = factory; }

    @Override
    public String ping() { return "pong"; }

    @Override
    public SearchOffersResponse searchOffers(SearchCriteria criteria) {
        AgencyCredentials creds = new AgencyCredentials(criteria.agence, null, null);
        List<Impl.Offer> offers = factory.findOffers(creds, criteria.arrivee.toGregorianCalendar().toZonedDateTime().toLocalDate(), criteria.depart.toGregorianCalendar().toZonedDateTime().toLocalDate(), criteria.nbPersonnes);
        OfferList dtoList = new OfferList();
        dtoList.setOffers(offers.stream().map(o -> {
            org.examples.server.dto.Offer of = new org.examples.server.dto.Offer();
            of.offerId = o.getId();
            of.nbLits = o.getBeds();
            try {
                of.start = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(o.getFrom().toString());
                of.end = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(o.getTo().toString());
            } catch (Exception e) { }
            of.prixTotal = (int)o.getPrice();
            return of;
        }).collect(Collectors.toList()));
        SearchOffersResponse resp = new SearchOffersResponse();
        resp.setOffers(dtoList);
        return resp;
    }

    @Override
    public ReservationConfirmation makeReservation(ReservationRequest request) throws ServiceFault {
        // Use nom/prenom/carte fields (ReservationRequest doesn't have client subobject here)
        String nom = request.nom;
        String prenom = request.prenom;
        String carte = request.carte;
        Client c = new Client(nom, prenom, carte);

        String agencyId = (request.auth != null) ? request.auth.agencyId : request.agence;
        String login = (request.auth != null) ? request.auth.login : null;
        String password = (request.auth != null) ? request.auth.password : null;

        ReservationResult res = factory.reserve(agencyId, login, password, request.offerId, c);
        ReservationConfirmation rc = new ReservationConfirmation();
        rc.setSuccess(res.isSuccess());
        rc.setMessage(res.getMessage());
        rc.setReference(res.getReference());
        return rc;
    }

    @Override
    public Catalog getCatalog() {
        Catalog cat = new Catalog();
        cat.name = factory.getHotelName();
        return cat;
    }
}
