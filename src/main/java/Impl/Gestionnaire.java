package Impl;

// Gestionnaire.java
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Gestionnaire {
    public static record Offre(Hotel hotel, Chambre chambre, int prixTotal) {}

    private final List<Hotel> hotels = new ArrayList<>();

    public void addHotel(Hotel h) { hotels.add(h); }
    public List<Hotel> getHotels() { return hotels; }

    // Recherche complète: ville, dates, prix min/max, catégorie (optionnelle), nbEtoiles (optionnel), nb personnes.
    public List<Offre> findMatchReservation(
            String ville, LocalDate dateArrivee, LocalDate dateDepart,
            Integer prixMin, Integer prixMax,
            Categorie categorie, Integer nbEtoiles,
            int nbPersonnes) {

        List<Offre> offres = new ArrayList<>();
        for (Hotel h : hotels) {
            if (!h.getAdresse().getVille().equalsIgnoreCase(ville)) continue;
            if (categorie != null && h.getCategorie() != categorie) continue;
            if (nbEtoiles != null && h.getNbEtoiles() != nbEtoiles) continue;

            for (Chambre c : h.chambresDisponibles(dateArrivee, dateDepart, nbPersonnes)) {
                int prix = c.prixTotal(dateArrivee, dateDepart);
                if (prixMin != null && prix < prixMin) continue;
                if (prixMax != null && prix > prixMax) continue;
                offres.add(new Offre(h, c, prix));
            }
        }
        offres.sort(Comparator.comparingInt(Offre::prixTotal));
        return offres;
    }

    public List<Chambre> findMatchReservationChambres(
            String ville, LocalDate dateArrivee, LocalDate dateDepart,
            Integer prixMin, Integer prixMax,
            Categorie categorie, Integer nbEtoiles,
            int nbPersonnes) {
        List<Chambre> res = new ArrayList<>();
        for (Offre o : findMatchReservation(ville, dateArrivee, dateDepart, prixMin, prixMax, categorie, nbEtoiles, nbPersonnes)) {
            res.add(o.chambre());
        }
        return res;
    }

    // Réaliser une réservation (simple)
    public Reservation makeReservation(Client client, Chambre chambre, LocalDate debut, LocalDate fin) {
        return chambre.reserver(client, debut, fin);
    }

    public void annuleReservation(Reservation r) {
        r.getChambre().getReservations().remove(r);
    }
}
