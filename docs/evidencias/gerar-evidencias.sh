#!/usr/bin/env bash
# Gera docs/evidencias/requisicoes-api.md chamando a API real (app rodando + MongoDB).
# Uso: BASE=http://localhost:8082 bash docs/evidencias/gerar-evidencias.sh
set -u
BASE="${BASE:-http://localhost:8080}"
SENHA="${SENHA:-Patrao@2026}"
SAIDA="$(dirname "$0")/requisicoes-api.md"

chamar() { # titulo, usuario ("-" = sem login), caminho
  local titulo="$1" usuario="$2" caminho="$3" auth=() corpo status
  [ "$usuario" != "-" ] && auth=(-u "$usuario:$SENHA")
  corpo=$(curl -s -w '\n%{http_code}' "${auth[@]}" "$BASE$caminho")
  status=$(tail -n1 <<<"$corpo"); corpo=$(sed '$d' <<<"$corpo")
  {
    echo "### $titulo"
    echo
    echo '```http'
    echo "GET $caminho"
    [ "$usuario" != "-" ] && echo "Authorization: Basic ($usuario)"
    echo '```'
    echo
    echo "**HTTP $status**"
    echo
    echo '```json'
    python -c 'import json,sys; print(json.dumps(json.loads(sys.stdin.read()), ensure_ascii=False, indent=2))' <<<"$corpo" 2>/dev/null || echo "$corpo"
    echo '```'
    echo
  } >>"$SAIDA"
  echo "$status  $titulo"
}

{
  echo "# Evidências: requisições reais à API do dashboard"
  echo
  echo "Gerado em $(date '+%d/%m/%Y %H:%M') por \`docs/evidencias/gerar-evidencias.sh\`, contra o app rodando"
  echo "com o perfil \`demo\` e MongoDB local. Os valores mudam conforme a data em que o script roda."
  echo
  echo "## Cenários de sucesso"
  echo
} >"$SAIDA"

chamar "Opções dos filtros (RF05)" gerente "/api/dashboard/filtros"
chamar "Resumo dos últimos 30 dias (RF01)" gerente "/api/dashboard/resumo"
chamar "Resumo filtrado por categoria Pizza (RF01 + RF05)" admin "/api/dashboard/resumo?tipo=Pizza"
chamar "Vendas por semana (RF02)" gerente "/api/dashboard/vendas?agrupamento=SEMANA"
chamar "Vendas por hora do dia, última semana (RF02)" gerente "/api/dashboard/vendas?agrupamento=HORA&inicio=$(date -d '-6 days' +%F)&fim=$(date +%F)"
chamar "Vendas por categoria (RF02)" gerente "/api/dashboard/categorias"
chamar "Situação dos pedidos (RF03)" gerente "/api/dashboard/status"
chamar "Situação só dos cancelados (RF03 + RF05)" gerente "/api/dashboard/status?status=CANCELADO"
chamar "Top 5 produtos por faturamento (RF04)" gerente "/api/dashboard/produtos/mais-vendidos?limite=5&ordenarPor=FATURAMENTO"
chamar "Período sem pedidos (dados vazios tratados)" gerente "/api/dashboard/resumo?inicio=2020-01-01&fim=2020-01-31"

echo "## Cenários de erro" >>"$SAIDA"; echo >>"$SAIDA"
chamar "Sem login → 401" - "/api/dashboard/resumo"
chamar "Atendente → 403" atendente "/api/dashboard/resumo"
chamar "Período invertido → 400 filtro-invalido" gerente "/api/dashboard/resumo?inicio=2026-11-10&fim=2026-11-01"
chamar "Período maior que 366 dias → 400 filtro-invalido" gerente "/api/dashboard/vendas?inicio=2024-01-01&fim=2026-01-01"
chamar "Data em formato errado → 400 formato-invalido" gerente "/api/dashboard/resumo?inicio=10/11/2026"
chamar "Status inexistente → 400 formato-invalido" gerente "/api/dashboard/status?status=VOANDO"
chamar "Limite fora da faixa → 400 filtro-invalido" gerente "/api/dashboard/produtos/mais-vendidos?limite=500"
echo "Arquivo gerado: $SAIDA"
