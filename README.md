# HotelSoap — Architecture distribuée (Hôtels SOAP + Agence TCP + Client CLI)

Application console (Client CLI) s’appuyant sur:
- 2 serveurs d’hôtels exposant des services SOAP (Rivage sur 8081, Opéra sur 8082)
- 1 serveur d’agence (TCP local sur 7070, pas de REST/SOAP), qui agrège les hôtels
- 1 client CLI qui dialogue avec l’agence (et non directement avec les hôtels)

Les hôtels appliquent des tarifs/agences distincts. Les recherches passent par la méthode du Gestionnaire et les réservations impactent la disponibilité réelle des chambres.

---

## Prérequis
- Java 8 (OpenJDK 8)
- Maven 3.8+
- Linux/Bash (commandes ci-dessous)

---

## Démarrage rapide — Build, Kill, Lancer les serveurs en arrière-plan, Vérifier, Lancer le client

### 1) Build
```bash
./mvnw -DskipTests=true clean package
```

### 2) Stopper d’anciens processus (libérer les ports)
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
# Agence: récupérer le catalogue (nom agence, villes, agences partenaires)
echo '{"op":"catalog.get"}' | nc -w 2 localhost 7070
```

### 5) Lancer le client CLI (via l’Agence)
```bash
./mvnw -pl client-cli -DskipTests=true exec:java \
  -Dexec.mainClass=org.examples.client.ClientMain \
  -Dagency.tcp.enabled=true
```

Saisie de test (exemple):
- Ville: 1 (Sète)
- Arrivée: 2025-11-20
- Départ:  2025-11-22
- Nb personnes: 2
- Choisir une offre, effectuer la réservation, relancer une recherche aux mêmes dates: le nombre d’offres doit diminuer si la chambre réservée n’est plus disponible.

### 6) Consulter les logs
```bash
tail -n 120 logs/agency.log
tail -n 120 logs/rivage.log
tail -n 120 logs/opera.log
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

---

## Raccourcis utiles
- Redémarrer uniquement Rivage:
```bash
fuser -k 8081/tcp 2>/dev/null || true
./mvnw -pl server-rivage -DskipTests=true spring-boot:run > logs/rivage.log 2>&1 & echo $! > /tmp/rivage.pid
```
- Vérifier l’agence:
```bash
echo '{"op":"catalog.get"}' | nc -w 2 localhost 7070
```
- Lancer à nouveau le client:
```bash
./mvnw -pl client-cli -DskipTests=true exec:java \
  -Dexec.mainClass=org.examples.client.ClientMain \
  -Dagency.tcp.enabled=true
```

---

## Dépannage
- Port déjà utilisé: stoppez les processus (cf. étape 2) et relancez.
- WSDL inaccessible: vérifiez les logs `logs/rivage.log` / `logs/opera.log` (recherche des traces `[SOAP]` et `[INIT]`).
- L’agence ne répond pas: `tail -n 200 logs/agency.log` (recherche des traces `[AGENCY-REQ]`, `[AGENCY->HOTEL]`, `[HOTEL->AGENCY]`).
- Client via Agence: si vous voyez "Relais brisé (pipe)", relancez l’agence (arrêt + redémarrage) puis relancez le client.
- Si les offres ne diminuent pas après réservation: relancez une recherche avec les mêmes dates; côté serveur hôtel, vérifiez `[REQ] makeReservation` puis `[RESP] reservation ok` et ensuite `[RESP] searchOffers returning N offers`.

---

## Script de lancement automatique (`lancement.sh`)

Un script pratique `lancement.sh` est fourni à la racine du projet pour automatiser les étapes du README : build, libération des ports, démarrage des serveurs en arrière-plan, attente des WSDL et lancement du client.

Emplacement : `./lancement.sh`

Permissions :
```bash
chmod +x lancement.sh
```

Usage basique (démarre tout et lance le client) :
```bash
./lancement.sh
```

Options disponibles :
- `--no-client` : démarre uniquement les serveurs (les serveurs restent en arrière-plan). Utile pour lancer les serveurs depuis un terminal et lancer le client plus tard.
- `--arret-propre` : lorsqu'elle est utilisée, le script installera un trap et arrêtera proprement les serveurs (kill des PID) à la fin du script ou quand vous faites Ctrl-C. Utile pour tests automatisés ou sessions temporaires.

Exemples :
- Démarrer serveurs puis lancer le client (comportement par défaut) :
```bash
./lancement.sh
```

- Démarrer seulement les serveurs (client non lancé) :
```bash
./lancement.sh --no-client
```

- Démarrer serveurs, lancer le client, puis arrêter proprement les serveurs quand le client se termine :
```bash
./lancement.sh --arret-propre
```

- Démarrer serveurs sans client puis arrêter proprement via Ctrl-C :
```bash
./lancement.sh --no-client --arret-propre
# Appuyez sur Ctrl-C quand vous voulez arrêter proprement les serveurs
```

Comportement interne important :
- Les PIDs des serveurs sont écrits dans `/tmp/rivage.pid`, `/tmp/opera.pid` et `/tmp/agency.pid`.
- Les logs des serveurs sont écrits dans le répertoire `./logs` (fichiers : `rivage.log`, `opera.log`, `agency.log`).
- Le script attend la disponibilité des WSDL des hôtels (timeout 90s) avant de lancer le client.
- Si vous préférez contrôler manuellement le démarrage/arrêt des serveurs, utilisez `--no-client` et ensuite arrêtez avec `kill $(cat /tmp/*.pid)` ou `fuser -k`.
