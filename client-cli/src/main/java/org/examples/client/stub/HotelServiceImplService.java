package org.examples.client.stub;

import java.net.URL;

public class HotelServiceImplService {
    public HotelServiceImplService(URL wsdl) { }
    public HotelService getHotelServiceImplPort() { return new HotelServiceFallback(); }
}

class HotelServiceFallback implements HotelService {
    @Override
    public Catalog getCatalog() { return new Catalog(); }
    @Override
    public SearchOffersPayload searchOffers(SearchCriteria criteria) { return new SearchOffersPayload(); }
    @Override
    public ReservationConfirmation makeReservation(ReservationRequest req) {
        ReservationConfirmation rc = new ReservationConfirmation();
        rc.setMessage("Fallback: service not connected");
        rc.setId("N/A");
        return rc;
    }
}
