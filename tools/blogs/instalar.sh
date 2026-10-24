#!/usr/bin/env bash
# Instala o blogs no PATH do WSL: link em ~/.local/bin apontando para o binário deste
# repositório. Rodar de novo depois de recompilar não é preciso (o link continua valendo).
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BIN="$DIR/bin/blogs"

if command -v go >/dev/null 2>&1; then
  echo "Go encontrado: compilando $(go version | awk '{print $3}')..."
  (cd "$DIR" && CGO_ENABLED=0 go build -trimpath -ldflags "-s -w" -o bin/blogs .)
elif [ ! -f "$BIN" ]; then
  echo "Sem Go no WSL e sem binário em $BIN. Instale o Go ou peça o binário." >&2
  exit 1
fi
chmod +x "$BIN" 2>/dev/null || true

mkdir -p "$HOME/.local/bin"
ln -sf "$BIN" "$HOME/.local/bin/blogs"

if ! echo ":$PATH:" | grep -q ":$HOME/.local/bin:"; then
  echo 'export PATH="$HOME/.local/bin:$PATH"' >> "$HOME/.bashrc"
  echo "~/.local/bin adicionado ao PATH no ~/.bashrc (abra um terminal novo ou rode: source ~/.bashrc)"
fi

command -v aws >/dev/null 2>&1 || echo "Atenção: o blogs usa o AWS CLI, que não está no PATH do WSL."
echo "Pronto: blogs -> $BIN"
