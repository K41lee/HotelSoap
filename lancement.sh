#!/usr/bin/env bash
set -euo pipefail

# Script de lancement complet (build + démarrage des serveurs + client via l'Agence)
# Emplacement: racine du projet HotelSoap

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$LOG_DIR"

# Helper
info(){ echo "[INFO] $*"; }
err(){ echo "[ERR] $*" >&2; }

# Parse options
NO_CLIENT=false
ARRET_PROPRE=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-client) NO_CLIENT=true; shift ;;
    --arret-propre) ARRET_PROPRE=true; shift ;;
    -h|--help) echo "Usage: $0 [--no-client] [--arret-propre]"; exit 0 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

# 1) Build
info "Compilation et packaging (maven)..."
./mvnw -DskipTests=true clean package

# 2) Stopper d'anciens processus (libérer les ports)
info "Libération des ports 8081,8082,8083,8084,7070 si nécessaires..."
fuser -k 8081/tcp 2>/dev/null || true
fuser -k 8082/tcp 2>/dev/null || true
fuser -k 8083/tcp 2>/dev/null || true
fuser -k 8084/tcp 2>/dev/null || true
fuser -k 7070/tcp 2>/dev/null || true

# utilities pour arrêter les serveurs
cleanup(){
  info "Arrêt des serveurs démarrés (si présents)..."
  if [ -f /tmp/rivage.pid ]; then
    pid=$(cat /tmp/rivage.pid 2>/dev/null || true)
    [ -n "$pid" ] && kill "$pid" 2>/dev/null || true
    rm -f /tmp/rivage.pid || true
    info "rivage stopped"
  fi
  if [ -f /tmp/opera.pid ]; then
    pid=$(cat /tmp/opera.pid 2>/dev/null || true)
    [ -n "$pid" ] && kill "$pid" 2>/dev/null || true
    rm -f /tmp/opera.pid || true
    info "opera stopped"
  fi
  if [ -f /tmp/agency.pid ]; then
    pid=$(cat /tmp/agency.pid 2>/dev/null || true)
    [ -n "$pid" ] && kill "$pid" 2>/dev/null || true
    rm -f /tmp/agency.pid || true
    info "agency stopped"
  fi
}

# 3) Démarrer les serveurs en arrière-plan
start_server(){
  local module="$1"
  local logfile="$2"
  local pidfile="$3"
  info "Démarrage de $module -> $logfile"
  ./mvnw -pl "$module" -DskipTests=true spring-boot:run > "$logfile" 2>&1 &
  local pid=$!
  echo "$pid" > "$pidfile"
  info "$module démarré (pid=$pid)"
}

start_server server-rivage "$LOG_DIR/rivage.log" /tmp/rivage.pid
start_server server-opera  "$LOG_DIR/opera.log"  /tmp/opera.pid
start_server agency-server  "$LOG_DIR/agency.log" /tmp/agency.pid

# Si demande d'arret propre, installer le trap
if [ "$ARRET_PROPRE" = true ]; then
  trap cleanup EXIT
  info "Option --arret-propre activée : les serveurs seront arrêtés proprement à la fin du script"
fi

# 4) Attendre que les WSDL des hôtels soient disponibles
wait_wsdl(){
  local url="$1"
  local timeout=${2:-90}
  local start=$(date +%s)
  info "Attente WSDL $url (timeout ${timeout}s)..."
  while true; do
    if curl -sSf --max-time 2 "$url" >/dev/null 2>&1; then
      info "WSDL disponible: $url"
      return 0
    fi
    now=$(date +%s)
    if (( now - start >= timeout )); then
      err "Timeout waiting for $url"
      return 1
    fi
    sleep 1
  done
}

# vérifier les WSDL
wait_wsdl "http://localhost:8081/hotel-rivage/hotel?wsdl" 90 || true
wait_wsdl "http://localhost:8083/hotel-opera/hotel?wsdl" 90 || true

info "Les serveurs devraient être démarrés (consultez les logs dans $LOG_DIR)."

# 5) Lancer le client (au premier plan) sauf si --no-client
if [ "$NO_CLIENT" = true ]; then
  info "--no-client : les serveurs ont été démarrés et le client n'est pas lancé."
  if [ "$ARRET_PROPRE" = true ]; then
    info "Appuyez sur Ctrl-C pour arrêter les serveurs proprement."
    # garder le script en vie pour permettre Ctrl-C
    while true; do sleep 3600; done
  else
    info "Les serveurs restent en arrière-plan. (PIDs: $(cat /tmp/rivage.pid 2>/dev/null || echo "-") $(cat /tmp/opera.pid 2>/dev/null || echo "-") $(cat /tmp/agency.pid 2>/dev/null || echo "-"))"
    exit 0
  fi
else
  info "Lancement du client CLI (au premier plan), pour quitter: Ctrl-C"
  ./mvnw -pl client-cli -DskipTests=true exec:java \
    -Dexec.mainClass=org.examples.client.ClientMain \
    -Dagency.tcp.enabled=true
fi

# Si on atteint ici, le client s'est terminé
info "Client terminé, script fini."

# Si --arret-propre était activé, le trap cleanup sera exécuté automatiquement à la sortie
