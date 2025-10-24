package org.examples.server.soap;

import org.examples.server.dto.*;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService(targetNamespace="http://service.hotel.examples.org/")
public interface HotelService {
    @WebMethod SearchOffersResponse searchOffers(SearchCriteria criteria);
    @WebMethod ReservationConfirmation makeReservation(ReservationRequest request) throws ServiceFault;
    @WebMethod Catalog getCatalog();
}
