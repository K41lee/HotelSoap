# HotelSoap — Architecture distribuée (Hôtels SOAP + Agence TCP + Client CLI)

Application console (Client CLI) s'appuyant sur:
- 2 serveurs d'hôtels exposant des services SOAP (Rivage sur 8081, Opéra sur 8082)
- 1 serveur d'agence (TCP local sur 7070, pas de REST/SOAP), qui agrège les hôtels
- 1 client CLI qui dialogue avec l'agence (et non directement avec les hôtels)
- **Bases de données H2** pour persister les données de chaque hôtel

Les hôtels appliquent des tarifs/agences distincts. Les recherches passent par la méthode du Gestionnaire et les réservations impactent la disponibilité réelle des chambres.

---

## Prérequis
- Java 8 (OpenJDK 8)
- Maven 3.8+
- Linux/Bash (commandes ci-dessous)

---

## Démarrage rapide

### 1) Build
```bash
./mvnw -DskipTests=true clean package
```

### 2) Stopper d'anciens processus (libérer les ports)
```bash
fuser -k 8081/tcp 2>/dev/null || true
fuser -k 8082/tcp 2>/dev/null || true
fuser -k 7070/tcp 2>/dev/null || true
```

### 3) Démarrer les serveurs (logs dans ./logs)
```bash
# Hôtel Rivage (SOAP sur 8081)
./mvnw -pl server-rivage -DskipTests=true spring-boot:run > logs/rivage.log 2>&1 & echo $! > /tmp/rivage.pid

# Hôtel Opéra (SOAP sur 8082)
./mvnw -pl server-opera  -DskipTests=true spring-boot:run > logs/opera.log  2>&1 & echo $! > /tmp/opera.pid

# Agence (TCP sur 7070, relai entre client et hôtels)
./mvnw -pl agency-server -DskipTests=true spring-boot:run > logs/agency.log 2>&1 & echo $! > /tmp/agency.pid
```

### 4) Vérifications rapides (WSDL + Agence TCP)
```bash
# WSDL Rivage
curl -sSf http://localhost:8081/hotel-rivage/hotel?wsdl | head -n1

# WSDL Opéra
curl -sSf http://localhost:8082/hotel-opera/hotel?wsdl | head -n1

# Agence: récupérer le catalogue
echo '{"op":"catalog.get"}' | nc -w 2 localhost 7070
```

### 5) Lancer le client CLI (via l'Agence)
```bash
./mvnw -pl client-cli -DskipTests=true exec:java \
  -Dexec.mainClass=org.examples.client.ClientMain \
  -Dagency.tcp.enabled=true
```

### 6) Arrêt propre
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

---

## 📊 Bases de données H2

Chaque hôtel dispose de sa propre base de données H2 pour persister les réservations et les données.

### Configuration

#### Hotel Opera
- **Port serveur**: 8082
- **Base de données**: `server-opera/data/hotel-opera-db.mv.db`
- **Console H2**: http://localhost:8082/h2-console
- **JDBC URL**: `jdbc:h2:file:./data/hotel-opera-db`
- **User**: `opera`
- **Password**: `opera`

#### Hotel Rivage
- **Port serveur**: 8081
- **Base de données**: `server-rivage/data/hotel-rivage-db.mv.db`
- **Console H2**: http://localhost:8081/h2-console
- **JDBC URL**: `jdbc:h2:file:./data/hotel-rivage-db`
- **User**: `rivage`
- **Password**: `rivage`

### Tables créées automatiquement

1. **hotels** - Informations sur l'hôtel (id, nom, ville, rue, numero, pays, categorie, nb_etoiles)
2. **chambres** - Chambres de l'hôtel (id, numero, nb_lits, prix_par_nuit, hotel_id)
3. **reservations** - Réservations des chambres (id, chambre_id, client_nom, client_prenom, client_carte, debut, fin, reference, agence)
4. **agences** - Agences partenaires (id, nom, reduction, login, password, hotel_id)

### Accès aux bases de données

#### Méthode 1: Console Web H2 (Serveurs lancés)

1. Ouvrir le navigateur sur http://localhost:8082/h2-console (Opera) ou http://localhost:8081/h2-console (Rivage)
2. **Configuration de connexion** :
   - **JDBC URL**: `jdbc:h2:file:./data/hotel-opera-db` (pour Opera)
   - **User Name**: `opera` (ou `rivage`)
   - **Password**: `opera` (ou `rivage`)
3. Cliquer sur **"Connect"**

⚠️ **IMPORTANT**: N'utilisez PAS l'URL par défaut `/home/etudiant/test`. Utilisez exactement `jdbc:h2:file:./data/hotel-opera-db`

#### Méthode 2: H2 Shell (Serveurs arrêtés)

Script automatique :
```bash
./view_database.sh
```

Ou manuellement :
```bash
# Pour Opera
cd server-opera
java -cp ~/.m2/repository/com/h2database/h2/2.1.214/h2-2.1.214.jar org.h2.tools.Shell \
  -url "jdbc:h2:file:./data/hotel-opera-db" \
  -user "opera" \
  -password "opera"

# Pour Rivage
cd server-rivage
java -cp ~/.m2/repository/com/h2database/h2/2.1.214/h2-2.1.214.jar org.h2.tools.Shell \
  -url "jdbc:h2:file:./data/hotel-rivage-db" \
  -user "rivage" \
  -password "rivage"
```

### Requêtes SQL utiles

```sql
-- Voir tous les hôtels
SELECT * FROM hotels;

-- Voir toutes les chambres
SELECT * FROM chambres;

-- Voir toutes les réservations
SELECT * FROM reservations;

-- Voir toutes les agences
SELECT * FROM agences;

-- Réservations avec détails
SELECT r.reference, r.client_nom, r.client_prenom, r.debut, r.fin, 
       c.numero as chambre, h.nom as hotel
FROM reservations r
JOIN chambres c ON r.chambre_id = c.id
JOIN hotels h ON c.hotel_id = h.id;

-- Quitter
exit
```

### Fonctionnalités

- **Synchronisation automatique** : Au démarrage, les données du domaine sont automatiquement synchronisées vers H2
- **Persistance des réservations** : Toutes les réservations SOAP sont sauvegardées en base de données
- **Recherche optimisée** : Requêtes JPA pour trouver les chambres disponibles sans conflit

### Limitation importante

⚠️ **H2 en mode fichier n'autorise qu'UNE SEULE connexion à la fois**

- **Serveurs lancés** → Utilisez la **Console Web**
- **Serveurs arrêtés** → Utilisez **H2 Shell** ou le **script view_database.sh**

---

## 🏗️ Architecture technique

### Structure du code

#### Entités JPA (server-base)
- `org.examples.server.entity.HotelEntity`
- `org.examples.server.entity.ChambreEntity`
- `org.examples.server.entity.ReservationEntity`
- `org.examples.server.entity.AgenceEntity`

#### Repositories Spring Data JPA
- `org.examples.server.repository.HotelRepository`
- `org.examples.server.repository.ChambreRepository`
- `org.examples.server.repository.ReservationRepository`
- `org.examples.server.repository.AgenceRepository`

#### Services
- `org.examples.server.service.HotelDatabaseService` - Gestion CRUD de la base de données
- `org.examples.server.service.DataSyncService` - Synchronisation au démarrage

### Dépendances ajoutées

Dans `server-base/pom.xml` :
```xml
<!-- H2 Database -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.1.214</version>
    <scope>runtime</scope>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Spring Boot Web (pour console H2) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

---

## 🔧 Scripts utiles

### view_database.sh
Script interactif pour accéder aux bases de données H2 quand les serveurs sont arrêtés.

```bash
./view_database.sh
```

### test_h2_config.sh
Script de validation de la configuration H2.

```bash
./test_h2_config.sh
```

---

## 📝 Logs

Les logs des serveurs sont disponibles dans le dossier `logs/` :
- `logs/opera.log` - Logs du serveur Opera
- `logs/rivage.log` - Logs du serveur Rivage
- `logs/agency.log` - Logs du serveur Agency

```bash
# Consulter les logs en temps réel
tail -f logs/opera.log
tail -f logs/rivage.log
tail -f logs/agency.log
```

---

## 🚨 Résolution de problèmes

### Erreur: "Database '/home/etudiant/test' not found"

**Cause** : URL JDBC incorrecte dans la console H2.

**Solution** : Utilisez exactement ces URLs :
- Opera : `jdbc:h2:file:./data/hotel-opera-db`
- Rivage : `jdbc:h2:file:./data/hotel-rivage-db`

### Erreur: "The file is locked"

**Cause** : La base de données est utilisée par un serveur en cours d'exécution.

**Solution** : 
- Pour consulter : Utilisez la **console web** (serveurs lancés)
- Pour H2 Shell : **Arrêtez d'abord les serveurs**

### Port déjà utilisé

**Solution** :
```bash
# Libérer les ports
fuser -k 8081/tcp 2>/dev/null || true
fuser -k 8082/tcp 2>/dev/null || true
fuser -k 7070/tcp 2>/dev/null || true
```

### Vérifier que les serveurs sont lancés

```bash
ps aux | grep spring-boot:run
```

### Vérifier les ports en écoute

```bash
ss -tuln | grep -E ":8081|:8082|:7070"
```

---

## 📚 Documentation supplémentaire

### WSDL des services

- **Rivage** : http://localhost:8081/hotel-rivage/hotel?wsdl
- **Opera** : http://localhost:8082/hotel-opera/hotel?wsdl

### Endpoints SOAP

Les serveurs exposent les opérations suivantes :
- `searchOffers` - Rechercher des offres disponibles
- `makeReservation` - Effectuer une réservation
- `getCatalog` - Obtenir le catalogue de l'hôtel

---

## 🎯 Workflow complet

1. **Build** : `./mvnw clean package -DskipTests`
2. **Démarrer serveurs** : Rivage, Opera, Agency (voir section Démarrage)
3. **Vérifier WSDL** : Accessible sur les URLs ci-dessus
4. **Vérifier BDD** : Les fichiers .mv.db sont créés dans les dossiers data/
5. **Lancer client** : Interagir via l'agence pour rechercher et réserver
6. **Consulter BDD** : Via console H2 ou H2 Shell pour voir les réservations persistées
7. **Arrêter** : Kill des processus proprement

---

## ✅ Validation

Pour vérifier que tout fonctionne :

1. **Serveurs démarrés** :
   ```bash
   curl http://localhost:8081/hotel-rivage/hotel?wsdl
   curl http://localhost:8082/hotel-opera/hotel?wsdl
   echo '{"op":"catalog.get"}' | nc -w 2 localhost 7070
   ```

2. **Bases de données créées** :
   ```bash
   ls -lh server-opera/data/hotel-opera-db.mv.db
   ls -lh server-rivage/data/hotel-rivage-db.mv.db
   ```

3. **Console H2 accessible** :
   - http://localhost:8082/h2-console
   - http://localhost:8081/h2-console

4. **Données synchronisées** :
   ```sql
   -- Via console H2 ou H2 Shell
   SELECT COUNT(*) FROM hotels;    -- Doit retourner 1
   SELECT COUNT(*) FROM chambres;  -- Doit retourner 2 ou plus
   ```

---

## 📦 Structure du projet

```
HotelSoap/
├── agency-server/          # Serveur agence (TCP sur 7070)
├── client-cli/             # Client en ligne de commande
├── domain/                 # Modèle de domaine partagé
├── server-base/            # Code commun des serveurs SOAP
│   └── src/main/java/org/examples/server/
│       ├── entity/         # Entités JPA
│       ├── repository/     # Repositories Spring Data
│       └── service/        # Services (Database, Sync)
├── server-opera/           # Serveur hotel Opera (8082)
│   └── data/               # Base de données H2 Opera
├── server-rivage/          # Serveur hotel Rivage (8081)
│   └── data/               # Base de données H2 Rivage
├── logs/                   # Logs des serveurs
├── scripts/                # Scripts utilitaires
├── view_database.sh        # Script d'accès BDD
├── test_h2_config.sh       # Script de validation H2
└── README.md               # Ce fichier
```

---

## 🔗 Technologies utilisées

- **Java 8** - Langage principal
- **Spring Boot 2.7.12** - Framework applicatif
- **JAX-WS / Metro 2.3.3** - Services SOAP
- **Spring Data JPA** - Accès aux données
- **H2 Database 2.1.214** - Base de données embarquée
- **Hibernate 5.6.15** - ORM
- **Maven** - Gestion de build

---

**Projet HotelSoap - Architecture SOAP avec persistance H2**
*Dernière mise à jour : 19 novembre 2025*

