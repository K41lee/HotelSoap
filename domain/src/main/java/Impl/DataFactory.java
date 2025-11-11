package Impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataFactory {
    private final String hotelName;

    private DataFactory(String hotelName) {
        this.hotelName = hotelName;
    }

    public static DataFactory rivage() {
        return new DataFactory("Rivage");
    }

    public List<Offer> findOffers(AgencyCredentials creds, LocalDate from, LocalDate to, int persons) {
        List<Offer> offers = new ArrayList<>();
        LocalDate d = from;
        while (!d.isAfter(to.minusDays(1))) {
            offers.add(new Offer(UUID.randomUUID().toString(), persons, d, d.plusDays(1), 100.0));
            d = d.plusDays(1);
        }
        // adapter prix si agency preferred
        if (creds != null && "preferred".equals(creds.getAgencyId())) {
            List<Offer> adapted = new ArrayList<>();
            for (Offer o : offers) {
                adapted.add(new Offer(o.getId(), o.getBeds(), o.getFrom(), o.getTo(), o.getPrice() * 0.9));
            }
            return adapted;
        }
        return offers;
    }

    public ReservationResult reserve(String agencyId, String login, String password, String offerId, Client mainPerson) {
        if (offerId == null || mainPerson == null) return ReservationResult.failure("Données manquantes");
        String ref = hotelName.substring(0, Math.min(4, hotelName.length())).toUpperCase() + "-" + UUID.randomUUID().toString();
        return ReservationResult.success(ref);
    }

    public String getHotelName() { return hotelName; }
}
