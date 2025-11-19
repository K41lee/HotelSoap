#!/bin/bash

# Script pour accéder aux bases de données H2 des hôtels

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║       ACCÈS AUX BASES DE DONNÉES H2 DES HÔTELS                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Détection du JAR H2
H2_JAR=$(find ~/.m2/repository/com/h2database/h2 -name "h2-*.jar" 2>/dev/null | head -1)

if [ -z "$H2_JAR" ]; then
    echo "❌ Le JAR H2 n'a pas été trouvé dans le dépôt Maven local."
    echo "   Essayez de compiler le projet d'abord: mvn compile"
    exit 1
fi

echo "✅ JAR H2 trouvé: $H2_JAR"
echo ""

# Menu de sélection
echo "Choisissez l'hôtel dont vous voulez voir la base de données:"
echo ""
echo "1) Hotel Opera"
echo "2) Hotel Rivage"
echo "3) Afficher les données avec des requêtes SQL"
echo "4) Quitter"
echo ""
read -p "Votre choix (1-4): " choice

case $choice in
    1)
        echo ""
        echo "🏨 Connexion à la base de données OPERA..."
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "URL: jdbc:h2:file:./server-opera/data/hotel-opera-db"
        echo "User: opera"
        echo "Password: opera"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        cd server-opera
        java -cp "$H2_JAR" org.h2.tools.Shell \
            -url "jdbc:h2:file:./data/hotel-opera-db" \
            -user "opera" \
            -password "opera"
        ;;
    2)
        echo ""
        echo "🏨 Connexion à la base de données RIVAGE..."
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "URL: jdbc:h2:file:./server-rivage/data/hotel-rivage-db"
        echo "User: rivage"
        echo "Password: rivage"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        cd server-rivage
        java -cp "$H2_JAR" org.h2.tools.Shell \
            -url "jdbc:h2:file:./data/hotel-rivage-db" \
            -user "rivage" \
            -password "rivage"
        ;;
    3)
        echo ""
        echo "📊 AFFICHAGE DES DONNÉES"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        read -p "Choisissez l'hôtel (1=Opera, 2=Rivage): " hotel_choice

        if [ "$hotel_choice" = "1" ]; then
            DB_URL="jdbc:h2:file:./server-opera/data/hotel-opera-db"
            DB_USER="opera"
            DB_PASS="opera"
            HOTEL_NAME="OPERA"
        elif [ "$hotel_choice" = "2" ]; then
            DB_URL="jdbc:h2:file:./server-rivage/data/hotel-rivage-db"
            DB_USER="rivage"
            DB_PASS="rivage"
            HOTEL_NAME="RIVAGE"
        else
            echo "❌ Choix invalide"
            exit 1
        fi

        echo ""
        echo "🏨 Données de l'hôtel $HOTEL_NAME"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""

        # Hôtels
        echo "📋 HÔTELS:"
        java -cp "$H2_JAR" org.h2.tools.Shell \
            -url "$DB_URL" -user "$DB_USER" -password "$DB_PASS" \
            -sql "SELECT * FROM hotels;"

        echo ""
        echo "🛏️  CHAMBRES:"
        java -cp "$H2_JAR" org.h2.tools.Shell \
            -url "$DB_URL" -user "$DB_USER" -password "$DB_PASS" \
            -sql "SELECT * FROM chambres;"

        echo ""
        echo "📅 RÉSERVATIONS:"
        java -cp "$H2_JAR" org.h2.tools.Shell \
            -url "$DB_URL" -user "$DB_USER" -password "$DB_PASS" \
            -sql "SELECT * FROM reservations;"

        echo ""
        echo "🏢 AGENCES:"
        java -cp "$H2_JAR" org.h2.tools.Shell \
            -url "$DB_URL" -user "$DB_USER" -password "$DB_PASS" \
            -sql "SELECT * FROM agences;"

        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        ;;
    4)
        echo "Au revoir!"
        exit 0
        ;;
    *)
        echo "❌ Choix invalide"
        exit 1
        ;;
esac

echo ""
echo "✅ Session terminée"

