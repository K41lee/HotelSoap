package org.examples.server.impl;

import javax.jws.WebService;
import org.examples.server.api.HotelAgencyService;
import org.examples.server.dto.*;
import org.examples.server.soap.ServiceFault;
import Impl.DataFactory;
import Impl.AgencyCredentials;
import Impl.Client;
import Impl.ReservationResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@WebService(endpointInterface = "org.examples.server.api.HotelAgencyService", serviceName = "HotelAgencyService")
@Component
public class HotelAgencyServiceImpl implements HotelAgencyService {

    private DataFactory factory = DataFactory.rivage();

    public void setDataFactory(DataFactory factory) { this.factory = factory; }

    @Override
    public OfferList checkAvailability(AvailabilityRequest req) throws ServiceFault {
        AgencyCredentials creds = new AgencyCredentials(req.getAgencyId(), req.getLogin(), req.getPassword());
        List<Impl.Offer> offers = factory.findOffers(creds, LocalDate.parse(req.getFrom()), LocalDate.parse(req.getTo()), req.getPersons());
        OfferList list = new OfferList();
        List<Offer> dtoOffers = new ArrayList<>();
        for (Impl.Offer o : offers) {
            Offer of = new Offer();
            of.offerId = o.getId();
            of.nbLits = o.getBeds();
            try {
                of.start = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(o.getFrom().toString());
                of.end = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(o.getTo().toString());
            } catch (Exception e) {
                // ignore
            }
            of.prixTotal = (int)o.getPrice();
            dtoOffers.add(of);
        }
        list.setOffers(dtoOffers);
        return list;
    }

    @Override
    public ReservationConfirmation book(ReservationRequest req) throws ServiceFault {
        // Use fields from ReservationRequest (nom/prenom/carte) and auth if present
        String nom = req.nom;
        String prenom = req.prenom;
        String carte = req.carte;
        Client c = new Client(nom, prenom, carte);
        String agencyId = (req.auth != null) ? req.auth.agencyId : null;
        String login = (req.auth != null) ? req.auth.login : null;
        String password = (req.auth != null) ? req.auth.password : null;

        ReservationResult res = factory.reserve(agencyId, login, password, req.offerId, c);
        ReservationConfirmation rc = new ReservationConfirmation();
        rc.setSuccess(res.isSuccess());
        rc.setMessage(res.getMessage());
        rc.setReference(res.getReference());
        return rc;
    }
}
