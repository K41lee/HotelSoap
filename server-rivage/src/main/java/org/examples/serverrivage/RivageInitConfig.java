package org.examples.serverrivage;

import Impl.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RivageInitConfig {
    @Bean("gestionnaireInit")
    public Gestionnaire gestionnaire() {
        Gestionnaire g = new Gestionnaire();

        Hotel h = new Hotel("Hôtel Rivage", new Adresse("France","Sète","Quai du Large",1,"",43.39,3.69), Categorie.HAUT_DE_GAMME, 4);
        h.addChambre(new Chambre(h, 101, 2, 120));
        h.addChambre(new Chambre(h, 102, 3, 150));
        //h.addAgence(new Agence("rivageAgency", 0.10));

        g.addHotel(h);
        return g;
    }
}
