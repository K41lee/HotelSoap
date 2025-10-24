# HotelSoap — Exercice 2 / Question 1 (version non distribuée)

Application **console** (Spring Boot) de réservation d’hôtels, conforme au sujet du TP — *version non distribuée, sans base de données*.  
La saisie utilisateur (ville, dates, prix, catégorie/étoiles, nb personnes) renvoie une **liste d’offres** (nom de l’hôtel, adresse complète, prix total, étoiles, lits), puis permet de **sélectionner** une offre et de **créer** la réservation en mémoire.

> La version distribuée (SOAP) sera traitée ultérieurement en **Question 2**. Ici tout se fait **dans le même processus**, sans appel réseau.

---

## Prérequis
- **Java 17**
- **Maven 3.8+**
- Environnement recommandé : **IntelliJ IDEA** (ou n’importe quel IDE Java)

---

## Lancer l’application

### Depuis IntelliJ
1. Ouvrir le projet.
2. Vérifier le **JDK 17** dans *Project Structure*.
3. Lancer la classe `com.example.hotel.HotelApplication` (clic droit > Run).
4. cd client-cli
   mvn -DskipTests clean package -Dwsdl.url=http://localhost:8080/hotelservice?wsdl
5. /usr/lib/jvm/java-8-openjdk-amd64/bin/java \
   -Dwsdl.url=http://localhost:8080/hotelservice?wsdl \
   -cp client-cli/target/client-cli-1.0.0.jar \
   org.examples.client.ClientMain

