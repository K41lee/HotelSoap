package cli;

import cli.*;
import Impl.*; // Adresse, Categorie, Chambre, Client, DateSimple, Gestionnaire, Hotel, Reservation
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataFactory {

    @Bean
    public Gestionnaire gestionnaire() {
        Gestionnaire g = new Gestionnaire();

        Hotel h1 = new Hotel("Hôtel Rivage",
                new Adresse("France","Montpellier","Rue de la Mer",12,"",43.61,3.88),
                Categorie.MILIEU_DE_GAMME, 3);
        h1.addChambre(new Chambre(h1,101,2,80));
        h1.addChambre(new Chambre(h1,102,4,120));

        Hotel h2 = new Hotel("Luxe Opéra",
                new Adresse("France","Montpellier","Bd Victor",5,"Quartier Opéra",43.61,3.89),
                Categorie.HAUT_DE_GAMME, 5);
        h2.addChambre(new Chambre(h2,201,2,220));
        h2.addChambre(new Chambre(h2,202,2,240));

        Hotel h3 = new Hotel("City Budget",
                new Adresse("France","Lyon","Rue Neuve",3,"",45.76,4.84),
                Categorie.ECONOMIQUE, 2);
        h3.addChambre(new Chambre(h3,1,2,55));
        h3.addChambre(new Chambre(h3,2,3,70));

        g.addHotel(h1); g.addHotel(h2); g.addHotel(h3);
        return g;
    }
}
