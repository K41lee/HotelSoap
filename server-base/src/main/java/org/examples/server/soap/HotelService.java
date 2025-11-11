package org.examples.server.soap;

import javax.jws.WebMethod;
import javax.jws.WebService;
import org.examples.server.dto.*;

@WebService(
        targetNamespace = "http://service.hotel.examples.org/",
        name = "HotelService"
)
public interface HotelService {

    @WebMethod
    String ping();

    @WebMethod
    SearchOffersResponse searchOffers(SearchCriteria criteria);
    @WebMethod
    ReservationConfirmation makeReservation(ReservationRequest request) throws ServiceFault;
    @WebMethod
    Catalog getCatalog();
}
