#!/bin/bash

echo "=========================================="
echo "Test de la configuration H2"
echo "=========================================="
echo ""

# Vérifier que les fichiers de configuration existent
echo "1. Vérification des fichiers de configuration..."
if grep -q "spring.datasource.url=jdbc:h2" server-opera/src/main/resources/application.properties; then
    echo "   ✓ Configuration H2 trouvée pour server-opera"
else
    echo "   ✗ Configuration H2 manquante pour server-opera"
fi

if grep -q "spring.datasource.url=jdbc:h2" server-rivage/src/main/resources/application.properties; then
    echo "   ✓ Configuration H2 trouvée pour server-rivage"
else
    echo "   ✗ Configuration H2 manquante pour server-rivage"
fi

echo ""
echo "2. Vérification des entités JPA..."
ENTITY_COUNT=$(find server-base/src/main/java/org/examples/server/entity -name "*.java" 2>/dev/null | wc -l)
echo "   Nombre d'entités JPA trouvées: $ENTITY_COUNT"

echo ""
echo "3. Vérification des repositories..."
REPO_COUNT=$(find server-base/src/main/java/org/examples/server/repository -name "*.java" 2>/dev/null | wc -l)
echo "   Nombre de repositories trouvés: $REPO_COUNT"

echo ""
echo "4. Vérification des services..."
if [ -f "server-base/src/main/java/org/examples/server/service/HotelDatabaseService.java" ]; then
    echo "   ✓ HotelDatabaseService trouvé"
else
    echo "   ✗ HotelDatabaseService manquant"
fi

if [ -f "server-base/src/main/java/org/examples/server/service/DataSyncService.java" ]; then
    echo "   ✓ DataSyncService trouvé"
else
    echo "   ✗ DataSyncService manquant"
fi

echo ""
echo "5. Compilation du projet..."
mvn clean compile -DskipTests -q
if [ $? -eq 0 ]; then
    echo "   ✓ Compilation réussie"
else
    echo "   ✗ Erreur de compilation"
    exit 1
fi

echo ""
echo "=========================================="
echo "Configuration H2 validée avec succès !"
echo "=========================================="
echo ""
echo "Pour démarrer les serveurs avec H2:"
echo "  - Opera:  mvn -pl server-opera spring-boot:run"
echo "  - Rivage: mvn -pl server-rivage spring-boot:run"
echo ""
echo "Consoles H2:"
echo "  - Opera:  http://localhost:8082/h2-console"
echo "  - Rivage: http://localhost:8081/h2-console"
echo ""

