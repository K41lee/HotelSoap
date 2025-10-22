package cli;

import Impl.Adresse;
import Impl.Categorie;     // enum en FULL CAPS
import Impl.Chambre;
import Impl.Client;
import Impl.Gestionnaire;
import Impl.Hotel;
import Impl.Reservation;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CliRunner implements CommandLineRunner {

    private final Gestionnaire g;

    public CliRunner(Gestionnaire gestionnaire) {
        this.g = gestionnaire;
    }

    @Override
    public void run(String... args) {
        Locale.setDefault(Locale.FRANCE);
        Scanner in = new Scanner(System.in);

        System.out.println("=== Réservation d’hôtels (Spring Boot CLI) ===");
        System.out.println("(Astuce : tapez '?' à une invite pour lister ce qui est disponible)");

        while (true) {

            // --- Saisie des critères ---
            String ville = askCity(in); // avec '?'
            LocalDate arrivee = askDate(in, "Date d'arrivée (YYYY-MM-DD)");
            LocalDate depart = askDate(in, "Date de départ  (YYYY-MM-DD)");
            Integer prixMin = askOptionalInt(in, "Prix min (Entrée pour ignorer)");
            Integer prixMax = askOptionalInt(in, "Prix max (Entrée pour ignorer)");
            Categorie categorie = askOptionalCategorie(in); // avec '?', FULL CAPS
            Integer nbEtoiles = askOptionalStars(in);       // avec '?'
            int nbPersonnes = askInt(in, "Nombre de personnes");

            // --- Recherche ---
            List<Gestionnaire.Offre> offres = g.findMatchReservation(
                    ville, arrivee, depart, prixMin, prixMax, categorie, nbEtoiles, nbPersonnes
            );

            if (offres == null || offres.isEmpty()) {
                System.out.println("\nAucune offre ne correspond aux critères.");
                return;
            }

            System.out.println("\nOffres trouvées :");
            for (int i = 0; i < offres.size(); i++) {
                var o = offres.get(i);
                Hotel h = o.hotel();
                Chambre c = o.chambre();
                Adresse a = h.getAdresse();
                System.out.printf(
                        "%d) %s — %s, %s %d, %s | ch.%d (%d lits) | %d★, %s | %d € [%s → %s]%n",
                        i + 1,
                        h.getNom(),
                        a.getVille(), a.getRue(), a.getNumero(), a.getPays(),
                        c.getNumero(), c.getNbLits(),
                        h.getNbEtoiles(), h.getCategorie(),
                        o.prixTotal(), arrivee, depart
                );
            }

            int choix = askIntRange(in, "\nChoisissez une offre", 1, offres.size());
            var selection = offres.get(choix - 1);

            // --- Saisie client ---
            System.out.println("\n=== Saisie client ===");
            String nom = askStr(in, "Nom");
            String prenom = askStr(in, "Prénom");
            String carte = askStr(in, "Carte de crédit (ex: 4111-1111-1111-1111)");

            Client client = new Client(nom, prenom, carte);

            // --- Réservation ---
            Reservation resa = g.makeReservation(client, selection.chambre(), arrivee, depart);

            System.out.printf(
                    "\n✅ Réservation confirmée : %s %s à l’hôtel %s, chambre %d, du %s au %s. Montant: %d €%n",
                    client.getPrenom(), client.getNom(),
                    resa.getChambre().getHotel().getNom(),
                    resa.getChambre().getNumero(),
                    arrivee, depart,
                    selection.prixTotal()
            );
        }
    }

    // ================= Helpers I/O =================

    private static String askStr(Scanner in, String label) {
        System.out.print(label + " : ");
        String s = in.nextLine().trim();
        while (s.isEmpty()) {
            System.out.print("Requis. " + label + " : ");
            s = in.nextLine().trim();
        }
        return s;
    }

    private static Integer askOptionalInt(Scanner in, String label) {
        System.out.print(label + " : ");
        String s = in.nextLine().trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            System.out.println("Valeur invalide, critère ignoré.");
            return null;
        }
    }

    private static int askInt(Scanner in, String label) {
        while (true) {
            System.out.print(label + " : ");
            String s = in.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Veuillez entrer un entier.");
            }
        }
    }

    private static int askIntRange(Scanner in, String label, int min, int max) {
        while (true) {
            int v = askInt(in, label + " [" + min + "-" + max + "]");
            if (v >= min && v <= max) return v;
            System.out.println("Hors bornes.");
        }
    }

    private static LocalDate askDate(Scanner in, String label) {
        while (true) {
            System.out.print(label + " : ");
            String s = in.nextLine().trim();
            try {
                return LocalDate.parse(s);
            } catch (Exception e) {
                System.out.println("Format attendu YYYY-MM-DD.");
            }
        }
    }

    // ======= Prompts "avec ?" =======

    /** Ville avec '?': liste les villes disponibles provenant des hôtels chargés. */
    private String askCity(Scanner in) {
        while (true) {
            System.out.print("Ville de séjour (tapez '?' pour lister) : ");
            String s = in.nextLine().trim();
            if (s.equals("?")) {
                var villes = g.getHotels().stream()
                        .map(h -> h.getAdresse().getVille())
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(v -> !v.isEmpty())
                        .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
                System.out.println("Villes disponibles : " + (villes.isEmpty() ? "(aucune)" : String.join(", ", villes)));
                continue; // repose la question
            }
            if (!s.isEmpty()) return s;
            System.out.println("Requis.");
        }
    }

    /** Catégorie optionnelle, FULL CAPS ; '?' pour lister, accepte NUMÉRO ou NOM (insensible à la casse/underscores). */
    private static Categorie askOptionalCategorie(Scanner in) {
        while (true) {
            System.out.print("Catégorie (Entrée pour ignorer, '?' pour lister) : ");
            String s = in.nextLine().trim();
            if (s.isEmpty()) return null;
            if (s.equals("?")) {
                Categorie[] vals = Categorie.values();
                for (int i = 0; i < vals.length; i++) {
                    System.out.printf("  %d) %s%n", i + 1, vals[i].name());
                }
                continue;
            }
            // Numéro de menu ?
            try {
                int idx = Integer.parseInt(s);
                Categorie[] vals = Categorie.values();
                if (idx >= 1 && idx <= vals.length) return vals[idx - 1];
                System.out.println("Indice invalide.");
                continue;
            } catch (NumberFormatException ignore) { /* pas un numéro, on traite comme texte */ }

            // Nom (case-insensitive, tolère espaces/traits)
            String key = s.toUpperCase(Locale.ROOT)
                    .replace(' ', '_')
                    .replace('-', '_');
            try {
                return Categorie.valueOf(key);
            } catch (IllegalArgumentException e) {
                System.out.println("Catégorie inconnue. Tapez '?' pour lister.");
            }
        }
    }

    /** Etoiles optionnelles ; '?' affiche les valeurs existantes dans les données. */
    private Integer askOptionalStars(Scanner in) {
        while (true) {
            System.out.print("Nombre d’étoiles exact (Entrée pour ignorer, '?' pour lister) : ");
            String s = in.nextLine().trim();
            if (s.isEmpty()) return null;
            if (s.equals("?")) {
                var stars = g.getHotels().stream()
                        .map(Hotel::getNbEtoiles)
                        .collect(Collectors.toCollection(TreeSet::new));
                System.out.println("Étoiles disponibles : " + (stars.isEmpty() ? "(aucune)" : stars));
                continue;
            }
            try {
                int v = Integer.parseInt(s);
                if (v <= 0) { System.out.println("Doit être > 0."); continue; }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Veuillez entrer un entier ou '?' pour lister.");
            }
        }
    }
}
