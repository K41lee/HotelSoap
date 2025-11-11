package org.examples.client.stub;

public interface HotelService {
    Catalog getCatalog();
    SearchOffersPayload searchOffers(SearchCriteria criteria);
    ReservationConfirmation makeReservation(ReservationRequest req);
}

