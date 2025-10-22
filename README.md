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
3. Lancer la classe `HotelCliApplication` (configuration *Application* ou `mvn spring-boot:run`).

### En ligne de commande
```bash
# A la racine du projet
./mvnw clean package -DskipTests
java -jar target/Hotel-0.0.1-SNAPSHOT.jar
./mvnw spring-boot:run

# Si besoin
./mvnw clean verify -DskipTests
./mvnw clean install -DskipTests
