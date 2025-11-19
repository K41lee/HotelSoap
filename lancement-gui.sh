#!/bin/bash

# Script de lancement de l'interface graphique du client d'hôtel

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║        🏨 Interface Graphique - Client d'Hôtel                ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Paramètres de l'agence
AGENCY_HOST=${AGENCY_HOST:-localhost}
AGENCY_PORT=${AGENCY_PORT:-7070}

echo "Configuration:"
echo "  • Agence: $AGENCY_HOST:$AGENCY_PORT"
echo ""

# Vérifier que l'agence est accessible
echo "Vérification de la connexion à l'agence..."
if ! nc -z $AGENCY_HOST $AGENCY_PORT 2>/dev/null; then
    echo "❌ ERREUR: L'agence n'est pas accessible sur $AGENCY_HOST:$AGENCY_PORT"
    echo ""
    echo "Assurez-vous que les serveurs sont lancés:"
    echo "  ./lancement.sh --no-client"
    echo ""
    exit 1
fi

echo "✅ Agence accessible"
echo ""
echo "Lancement de l'interface graphique..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Lancement de l'interface graphique
./mvnw -pl client-cli exec:java@run-gui \
    -Dagency.tcp.host=$AGENCY_HOST \
    -Dagency.tcp.port=$AGENCY_PORT

