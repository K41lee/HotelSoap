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

---

## Exécution rapide — Hôtels (SOAP) + Agence (TCP) + Client CLI

Ces commandes fonctionnent sous Linux (bash). Elles compilent, arrêtent les éventuels processus existants, démarquent les deux serveurs d’hôtels et l’agence en arrière‑plan, vérifient les endpoints, puis lancent le client via l’agence.

### 1) Build
```bash
./mvnw -DskipTests=true clean package
```

### 2) Arrêter les processus existants (ports 8081, 8082, 7070)
```bash
fuser -k 8081/tcp 2>/dev/null || true
fuser -k 8082/tcp 2>/dev/null || true
fuser -k 7070/tcp 2>/dev/null || true
```

### 3) Démarrer les serveurs en arrière‑plan (logs dans ./logs)
```bash
# Hôtel Rivage (SOAP sur 8081)
./mvnw -pl server-rivage -DskipTests=true spring-boot:run > logs/rivage.log 2>&1 & echo $! > /tmp/rivage.pid

# Hôtel Opéra (SOAP sur 8082)
./mvnw -pl server-opera  -DskipTests=true spring-boot:run > logs/opera.log  2>&1 & echo $! > /tmp/opera.pid

# Agence (TCP sur 7070, agrège les deux hôtels)
./mvnw -pl agency-server -DskipTests=true spring-boot:run > logs/agency.log 2>&1 & echo $! > /tmp/agency.pid
```

### 4) Vérifications rapides (WSDL + Agence TCP)
```bash
# WSDL Rivage
curl -sSf http://localhost:8081/hotel-rivage/hotel?wsdl | head -n1
# WSDL Opéra
curl -sSf http://localhost:8082/hotel-opera/hotel?wsdl | head -n1
# Agence: récupérer le catalogue (villes, agences)
echo '{"op":"catalog.get"}' | nc -w 2 localhost 7070
```

### 5) Lancer le client CLI (via l’Agence)
```bash
./mvnw -pl client-cli -DskipTests=true exec:java \
  -Dexec.mainClass=org.examples.client.ClientMain \
  -Dagency.tcp.enabled=true
```

Saisie conseillée pour un test rapide (doit renvoyer des offres):
- Ville: 1 (Sète) ou 2 (Montpellier)
- Arrivée: 2025-11-20
- Départ:  2025-11-22
- Nb personnes: 2

### 6) Consulter les logs
```bash
tail -n 80 logs/agency.log
Tail -n 80 logs/rivage.log
Tail -n 80 logs/opera.log
```

### 7) Arrêt propre
```bash
# Avec les PID mémorisés
kill $(cat /tmp/rivage.pid 2>/dev/null) 2>/dev/null || true
kill $(cat /tmp/opera.pid  2>/dev/null) 2>/dev/null || true
kill $(cat /tmp/agency.pid 2>/dev/null) 2>/dev/null || true

# Ou via ports (force)
fuser -k 8081/tcp 2>/dev/null || true
fuser -k 8082/tcp 2>/dev/null || true
fuser -k 7070/tcp 2>/dev/null || true
```

Notes:
- Les serveurs et le client compilent pour Java 8 via Maven toolchains; assurez‑vous d’avoir un JDK 8 installé si nécessaire.
- Les logs détaillent les requêtes/réponses: côté hôtels ([REQ]/[FILTER]/[MAP]/[RESP]), côté agence ([AGENCY-REQ]/[AGENCY->HOTEL]/[HOTEL->AGENCY]).
