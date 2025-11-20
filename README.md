# 🏨 HotelSoap — Système de Réservation d'Hôtels

Système distribué de réservation d'hôtels avec architecture SOAP et persistance H2.

## 📋 Architecture

- **2 serveurs d'hôtels** (SOAP + Spring Boot + H2)
  - **Hotel Rivage** : SOAP 8081, Web 8082
  - **Hotel Opera** : SOAP 8083, Web 8084
- **1 agence centrale** : TCP/JSON sur port 7070
- **Clients** : Interface graphique (Swing) ou CLI

---

## ⚡ Démarrage Ultra-Rapide

```bash
# Tout en une commande (GUI par défaut)
./lancement.sh

# Client en ligne de commande
./lancement.sh --no-gui

# Serveurs uniquement
./lancement.sh --no-client
```

### Prérequis
- Java 8+
- Maven 3.8+

---

## 🌐 URLs Importantes

| Service         | URL                                            |
|-----------------|------------------------------------------------|
| Rivage WSDL     | http://localhost:8081/hotel-rivage/hotel?wsdl  |
| Opera WSDL      | http://localhost:8083/hotel-opera/hotel?wsdl   |
| Rivage Console  | http://localhost:8082/h2-console               |
| Opera Console   | http://localhost:8084/h2-console               |
| Agence          | localhost:7070 (TCP/JSON)                      |

---

## 🗄️ Bases de Données H2

### Accès aux consoles (serveurs lancés)

| Hôtel   | Console                          | JDBC URL                              | Credentials       |
|---------|----------------------------------|---------------------------------------|-------------------|
| Opera   | http://localhost:8084/h2-console | `jdbc:h2:file:./data/hotel-opera-db`  | opera / opera     |
| Rivage  | http://localhost:8082/h2-console | `jdbc:h2:file:./data/hotel-rivage-db` | rivage / rivage   |

⚠️ **Important** : Utilisez exactement les JDBC URLs ci-dessus dans la console H2.

### Accès via script (serveurs arrêtés)
```bash
./view_database.sh
```

### Tables
- `hotels` : Informations des hôtels
- `chambres` : Chambres disponibles
- `reservations` : Réservations clients
- `agences` : Agences partenaires

---

## 🎨 Interface Graphique

```bash
# Lancer l'interface
./lancement.sh

# Ou manuellement
./lancement-gui.sh
```

**Fonctionnalités** :
- 🏠 Écran de bienvenue
- 🔍 Recherche avec calendrier
- 📊 Tableau de résultats
- 🖼️ **Visualisation des images de chambres** (clic sur colonne "Image")
- ✅ Formulaire de réservation
- 💰 Calcul automatique du prix

**Workflow** : Bienvenue → Recherche → Résultats → [Voir Image] → Réservation → Confirmation

---

## 🔧 Scripts Disponibles

| Script              | Description                              |
|---------------------|------------------------------------------|
| `./lancement.sh`    | Démarrer tout (GUI par défaut)          |
| `./lancement-gui.sh`| Lancer uniquement l'interface graphique  |
| `./view_database.sh`| Accéder aux bases de données            |

### Options lancement.sh
```bash
./lancement.sh              # GUI (défaut)
./lancement.sh --no-gui     # CLI
./lancement.sh --no-client  # Serveurs uniquement
./lancement.sh --help       # Aide
```

---

## 📝 Logs

```bash
# Consulter les logs
tail -f logs/opera.log
tail -f logs/rivage.log
tail -f logs/agency.log
```

---

## 🚨 Dépannage Rapide

### Ports occupés
```bash
fuser -k 8081/tcp 8082/tcp 8083/tcp 8084/tcp 7070/tcp
```

### Arrêter tous les serveurs
```bash
pkill -f "spring-boot:run"
```

### Console H2 : "Database not found"
✅ Utilisez : `jdbc:h2:file:./data/hotel-opera-db`  
❌ N'utilisez PAS : `/home/etudiant/test`

### Vérifier que tout fonctionne
```bash
# WSDL accessibles
curl -s http://localhost:8081/hotel-rivage/hotel?wsdl | head -1
curl -s http://localhost:8083/hotel-opera/hotel?wsdl | head -1

# Agence répond
echo '{"op":"catalog.get"}' | nc -w 2 localhost 7070
```

---

## 📦 Technologies

- **Java 8** + **Maven**
- **Spring Boot 2.7.12** + **Spring Data JPA**
- **JAX-WS Metro 2.3.3** (SOAP)
- **H2 Database 2.1.214**
- **Java Swing** + **JCalendar 1.4** (GUI)

---

## 📚 Documentation Complémentaire

- `GUI_README.txt` - Guide détaillé de l'interface graphique
- `DEMARRAGE_RAPIDE.txt` - Guide visuel pas-à-pas
- `ARCHITECTURE.txt` - Diagrammes et architecture technique
- `SYNTHESE_CORRECTIONS_GUI_FINALE.txt` - Historique des corrections

---

## 🎯 Workflow Complet

1. **Démarrer** : `./lancement.sh`
2. **Rechercher** : Ville, dates, lits
3. **Sélectionner** : Choisir une offre
4. **Réserver** : Nom, prénom, carte
5. **Confirmer** : Message de succès + référence
6. **Vérifier** : Console H2 → table `reservations`

---

**Projet HotelSoap v2.2.0**  
*Dernière mise à jour : 20 novembre 2025*

