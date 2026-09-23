#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Módulo 4 – Reproduce el escenario de la prueba en un repositorio temporal:
#   - Trabajo en curso en feature/new-login.
#   - Un compañero fusiona en main un fix de seguridad crítico junto con otros cambios.
#   - Se incorpora SOLO el fix en feature/new-login con `git cherry-pick -x`.
# También muestra el caso en que el fix llegó a main como merge commit de un PR (-m 1).
#
# Uso:  bash cherry-pick-demo.sh
# -----------------------------------------------------------------------------
set -euo pipefail

DEMO_DIR="$(mktemp -d)"
trap 'rm -rf "$DEMO_DIR"' EXIT
cd "$DEMO_DIR"

paso() { printf '\n\033[1;34m$ %s\033[0m\n' "$*"; "$@"; }
titulo() { printf '\n\033[1;32m=== %s ===\033[0m\n' "$*"; }
commit() { git add -A && git commit -q -m "$1"; }

git init -q -b main
git config user.name "Demo"
git config user.email "demo@example.com"
git config advice.detachedHead false
git config core.autocrlf false

titulo "Estado inicial de main"
cat > auth.py <<'EOF'
def validar_token(token):
    return token is not None
EOF
cat > reportes.py <<'EOF'
def generar():
    return "v1"
EOF
commit "feat: modulo de autenticacion y reportes"

titulo "Trabajo propio en feature/new-login"
git switch -q -c feature/new-login
cat > login.py <<'EOF'
def login(usuario, clave):
    return usuario == "admin" and clave == "secreta"
EOF
commit "feat(login): nuevo formulario de login"

titulo "Mientras tanto, en main: fix de seguridad + otros cambios que NO queremos"
git switch -q main
cat > reportes.py <<'EOF'
def generar():
    return "v2 - refactor grande aun no probado con login"
EOF
commit "refactor(reportes): nuevo motor de reportes"
cat > auth.py <<'EOF'
import hmac

def validar_token(token, esperado):
    # Fix de seguridad: comparacion en tiempo constante y validacion real del token
    return token is not None and hmac.compare_digest(token, esperado)
EOF
commit "fix(security): validar token con comparacion en tiempo constante (CVE interno)"
FIX_SHA="$(git rev-parse HEAD)"
echo "VERSION=2.0" > config.txt
commit "chore: subir version a 2.0"

paso git log --oneline --graph --all

titulo "1) Identificar el commit exacto del fix en main"
paso git log main --oneline --grep='security'
paso git show --stat --oneline "$FIX_SHA"

titulo "2) Traer SOLO ese commit a feature/new-login"
paso git switch feature/new-login
paso git cherry-pick -x "$FIX_SHA"

titulo "3) Verificar resultado"
paso git log --oneline --graph --all
paso git log -1 --format='%B'
echo "reportes.py en la feature (debe seguir en v1):"; cat reportes.py
echo "config.txt en la feature (no debe existir):"; ls config.txt 2>/dev/null || echo "  -> no existe, correcto"
echo "auth.py en la feature (debe tener el fix):"; cat auth.py

titulo "4) Caso alterno: el fix llegó a main como MERGE COMMIT de un Pull Request"
git switch -q main
git switch -q -c hotfix/sanitizar-usuario
cat > auth.py <<'EOF'
import hmac

def validar_token(token, esperado):
    # Fix de seguridad: comparacion en tiempo constante y validacion real del token
    return token is not None and hmac.compare_digest(token, esperado)

def sanitizar(usuario):
    return usuario.strip().lower()
EOF
commit "fix(security): sanitizar nombre de usuario"
git switch -q main
git merge -q --no-ff hotfix/sanitizar-usuario -m "Merge pull request #42 from hotfix/sanitizar-usuario"
MERGE_SHA="$(git rev-parse HEAD)"
git switch -q feature/new-login
echo "Intentar cherry-pick de un merge commit sin indicar el padre falla:"
paso git cherry-pick -x "$MERGE_SHA" || true
echo "Con -m 1 se toma el cambio que el PR introdujo respecto a main (padre 1):"
paso git cherry-pick -x -m 1 "$MERGE_SHA"
paso git log --oneline --graph feature/new-login -5

titulo "5) Al integrar la feature en main, Git reconoce los parches ya aplicados"
paso git switch main
paso git merge --no-ff feature/new-login -m "Merge feature/new-login"
paso git log --oneline --graph -8

titulo "Demo finalizada (el repositorio temporal se elimina automáticamente)"
