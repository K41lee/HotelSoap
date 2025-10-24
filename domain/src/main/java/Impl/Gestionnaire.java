package Impl;

// Gestionnaire.java

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    public int computePrixTotal(Chambre chambre, LocalDate debut, LocalDate fin, Optional<Agence> agenceOpt) {
        int base = chambre.prixTotal(debut, fin);
        if (agenceOpt.isPresent()) {
            double reduc = agenceOpt.get().getReduction(); // 0..1
            double after = base * (1.0 - reduc);
            return (int)Math.round(after);
        }
        return base;
    }

    public List<Offre> findMatchReservation(
            String ville, LocalDate dateArrivee, LocalDate dateDepart,
            Integer prixMin, Integer prixMax,
            Categorie categorie, Integer nbEtoiles,
            int nbPersonnes,
            String agenceName // <- NEW param
    ) {

        List<Offre> offres = new ArrayList<>();
        for (Hotel h : hotels) {
            if (!h.getAdresse().getVille().equalsIgnoreCase(ville)) continue;
            if (categorie != null && h.getCategorie() != categorie) continue;
            if (nbEtoiles != null && h.getNbEtoiles() != nbEtoiles) continue;

            // si agence demandée, tenter de la trouver sur l'hôtel
            Optional<Agence> agenceOpt = (agenceName == null || agenceName.isBlank())
                    ? Optional.empty()
                    : h.findAgenceByName(agenceName);

            for (Chambre c : h.getChambres()) {
                if (c.getNbLits() < nbPersonnes) continue;
                if (!c.isDisponible(dateArrivee, dateDepart)) continue;

                int prixApresReduc = computePrixTotal(c, dateArrivee, dateDepart, agenceOpt);

                if (prixMin != null && prixApresReduc < prixMin) continue;
                if (prixMax != null && prixApresReduc > prixMax) continue;

                offres.add(new Offre(h, c, prixApresReduc));
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
