package org.examples.hotel;

import Impl.*;

import java.time.LocalDate;
import java.util.List;

public class Demo {
    public static void main(String[] args) {
        Gestionnaire g = new Gestionnaire();

        // Données en dur
        Hotel h1 = new Hotel(
                "Hôtel Rivage",
                new Adresse("France", "Montpellier", "Rue de la Mer", 12, "", 43.61, 3.88),
                Categorie.MILIEU_DE_GAMME, 3
        );
        Chambre h1c101 = new Chambre(h1, 101, 2, 80);
        Chambre h1c102 = new Chambre(h1, 102, 4, 120);
        h1.addChambre(h1c101); h1.addChambre(h1c102);

        Hotel h2 = new Hotel(
                "Luxe Opéra",
                new Adresse("France", "Montpellier", "Bd Victor", 5, "Quartier Opéra", 43.61, 3.89),
                Categorie.HAUT_DE_GAMME, 5
        );
        Chambre h2c201 = new Chambre(h2, 201, 2, 220);
        h2.addChambre(h2c201);

        g.addHotel(h1); g.addHotel(h2);

        // Réservation existante pour illustrer "multiples réservations non chevauchées"
        Client alice = new Client("Durand", "Alice", "4111-1111-1111-1111");
        h1c101.reserver(alice, LocalDate.of(2025,10,1), LocalDate.of(2025,10,5)); // [1,5)

        // Recherche
        LocalDate arrivee = LocalDate.of(2025,10,7);
        LocalDate depart  = LocalDate.of(2025,10,9);
        List<Gestionnaire.Offre> offres = g.findMatchReservation(
                "Montpellier", arrivee, depart,
                50, 300, null, null, 2
        );

        System.out.println("Offres trouvées:");
        for (var o : offres) {
            System.out.printf("- %s, chambre #%d (%d lits) → %d € pour %s→%s%n",
                    o.hotel().getNom(), o.chambre().getNumero(), o.chambre().getNbLits(),
                    o.prixTotal(), arrivee, depart);
        }

        // Réserver l’une des offres
        if (!offres.isEmpty()) {
            Client bob = new Client("Martin", "Bob", "5555-1234-5678-9999");
            var choix = offres.get(0);
            var res = g.makeReservation(bob, choix.chambre(), arrivee, depart);
            System.out.println("Réservation confirmée pour " + bob.getPrenom() +
                    " à l'hôtel " + res.getChambre().getHotel().getNom() +
                    ", chambre " + res.getChambre().getNumero());
        }
    }
}
