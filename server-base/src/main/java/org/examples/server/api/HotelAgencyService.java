package org.examples.server.api;

import javax.jws.WebMethod;
import javax.jws.WebService;
import org.examples.server.dto.AvailabilityRequest;
import org.examples.server.dto.OfferList;
import org.examples.server.dto.ReservationConfirmation;
import org.examples.server.dto.ReservationRequest;
import org.examples.server.soap.ServiceFault;
import org.springframework.stereotype.Service;

@Service
@WebService(
        targetNamespace = "http://service.hotel.examples.org/",
        name = "HotelAgencyService"
)
public interface HotelAgencyService {

    @WebMethod
    OfferList checkAvailability(AvailabilityRequest req) throws ServiceFault;

    @WebMethod
    ReservationConfirmation book(ReservationRequest req) throws ServiceFault;
}
