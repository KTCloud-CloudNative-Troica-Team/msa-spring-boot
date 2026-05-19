#!/usr/bin/env bash
#
# dummy.bash — seed lots of fake products & inventory through admin-api-gateway.
#
# Pipeline:
#   1. signup an admin candidate via user-api-gateway (gets USER role)
#   2. promote that user to ADMIN directly in auth_db (admin gateway requires ADMIN)
#   3. signin via admin-api-gateway and capture access token
#   4. loop: POST /products → POST /inventories (1..N SKUs) → POST /increase
#
# Requirements: bash, curl, jq, docker (for the role-promotion step).
# All knobs come from env vars; defaults match container-compose.yaml + .env.example.
#
# Set DEBUG=1 to dump every request/response on failure (and on the first 2 of each kind).

set -euo pipefail

USER_GATEWAY="${USER_GATEWAY:-http://localhost:8100}"
ADMIN_GATEWAY="${ADMIN_GATEWAY:-http://localhost:8101}"

ADMIN_EMAIL="${ADMIN_EMAIL:-seed-admin@local}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin1234!}"
ADMIN_NAME="${ADMIN_NAME:-Seed Admin}"

AUTH_DB_CONTAINER="${AUTH_DB_CONTAINER:-auth-service-postgresql-db}"
AUTH_DB_USER="${AUTH_DB_USERNAME:-auth-service}"
AUTH_DB_NAME="${AUTH_DB_NAME:-auth_db}"

PRODUCT_COUNT="${PRODUCT_COUNT:-30}"
SKUS_PER_PRODUCT_MIN="${SKUS_PER_PRODUCT_MIN:-1}"
SKUS_PER_PRODUCT_MAX="${SKUS_PER_PRODUCT_MAX:-3}"
STOCK_MIN="${STOCK_MIN:-50}"
STOCK_MAX="${STOCK_MAX:-500}"
PRICE_MIN="${PRICE_MIN:-1000}"
PRICE_MAX="${PRICE_MAX:-200000}"

DEBUG="${DEBUG:-0}"

for bin in curl jq docker; do
  command -v "$bin" >/dev/null 2>&1 || { echo "[!] '$bin' is required but not on PATH" >&2; exit 1; }
done

rand_between() { # min max
  local min=$1 max=$2
  echo $(( RANDOM % (max - min + 1) + min ))
}

# api_call <label> <method> <url> [body] [extra_header]
# Echoes the response body on success. Dumps body + status on failure and exits.
api_call() {
  local label=$1 method=$2 url=$3 body=${4:-} extra_header=${5:-}
  local body_file status
  body_file=$(mktemp)
  local -a args=(-sS -o "$body_file" -w "%{http_code}" -X "$method" "$url" -H 'Content-Type: application/json')
  [[ -n "$extra_header" ]] && args+=(-H "$extra_header")
  [[ -n "$body" ]] && args+=(--data-raw "$body")

  status=$(curl "${args[@]}" || echo "000")

  if [[ "$DEBUG" == "1" ]]; then
    echo "    [debug] $label $method $url -> $status" >&2
    [[ -n "$body" ]] && echo "            req: $body" >&2
    echo "            resp: $(cat "$body_file")" >&2
  fi

  if [[ ! "$status" =~ ^2 ]]; then
    echo "" >&2
    echo "[!] $label failed: HTTP $status" >&2
    echo "    $method $url" >&2
    [[ -n "$body" ]] && echo "    request body : $body" >&2
    echo    "    response body: $(cat "$body_file")" >&2
    rm -f "$body_file"
    exit 1
  fi

  cat "$body_file"
  rm -f "$body_file"
}

CATEGORIES=("Outerwear" "Sneakers" "Mug" "Headphones" "Backpack" "Notebook" "Lamp" "Bottle" "Keyboard" "Chair" "T-shirt" "Hoodie" "Cap" "Jeans" "Watch")
ADJECTIVES=("Classic" "Premium" "Vintage" "Modern" "Eco" "Limited" "Signature" "Essential" "Pro" "Lite")

echo "==> 1/4  ensure admin user exists  ($ADMIN_EMAIL)"
signup_body=$(jq -n --arg e "$ADMIN_EMAIL" --arg p "$ADMIN_PASSWORD" --arg n "$ADMIN_NAME" \
  '{email:$e, password:$p, name:$n}')
signup_status=$(curl -sS -o /tmp/dummy-signup.out -w "%{http_code}" \
  -H 'Content-Type: application/json' \
  -X POST "$USER_GATEWAY/api/v1/auth/signup" \
  -d "$signup_body" || true)
case "$signup_status" in
  200|201) echo "    signup OK" ;;
  409|400) echo "    signup skipped (already exists: HTTP $signup_status)" ;;
  *)       echo "    signup returned HTTP $signup_status — continuing anyway"
           cat /tmp/dummy-signup.out 2>/dev/null || true
           echo ;;
esac

echo "==> 2/4  promote to ADMIN in $AUTH_DB_NAME"
docker exec -i "$AUTH_DB_CONTAINER" \
  psql -v ON_ERROR_STOP=1 -U "$AUTH_DB_USER" -d "$AUTH_DB_NAME" -q -c \
  "UPDATE users SET role='ADMIN' WHERE email='${ADMIN_EMAIL//\'/\'\'}';" \
  >/dev/null
echo "    role=ADMIN"

echo "==> 3/4  signin via admin-api-gateway"
signin_body=$(jq -n --arg e "$ADMIN_EMAIL" --arg p "$ADMIN_PASSWORD" '{email:$e, password:$p}')
signin_resp=$(api_call "signin" POST "$ADMIN_GATEWAY/admin/api/v1/auth/signin" "$signin_body")
ACCESS_TOKEN=$(echo "$signin_resp" | jq -r '.token.accessToken')
if [[ -z "$ACCESS_TOKEN" || "$ACCESS_TOKEN" == "null" ]]; then
  echo "[!] failed to extract accessToken from signin response:" >&2
  echo "$signin_resp" >&2
  exit 1
fi
AUTH_HEADER="Authorization: Bearer $ACCESS_TOKEN"
echo "    token acquired (len=${#ACCESS_TOKEN})"

echo "==> 4/4  seed $PRODUCT_COUNT products"
created_products=0
created_inventories=0
total_stock=0

for ((i = 1; i <= PRODUCT_COUNT; i++)); do
  category=${CATEGORIES[RANDOM % ${#CATEGORIES[@]}]}
  adjective=${ADJECTIVES[RANDOM % ${#ADJECTIVES[@]}]}
  serial=$(printf "%04d" "$i")
  pname="${adjective} ${category} ${serial}"
  pdesc="Auto-seeded ${category,,} for load/UI testing — batch $(date +%s)-${i}"
  price=$(rand_between "$PRICE_MIN" "$PRICE_MAX")

  product_body=$(jq -n --arg n "$pname" --arg d "$pdesc" --argjson p "$price" \
    '{name:$n, description:$d, price:$p}')
  product_resp=$(api_call "product[$i] create" POST \
    "$ADMIN_GATEWAY/admin/api/v1/products" "$product_body" "$AUTH_HEADER")
  product_id=$(echo "$product_resp" | jq -r '.product.id')
  if [[ -z "$product_id" || "$product_id" == "null" ]]; then
    echo "[!] product create succeeded but id missing: $product_resp" >&2
    exit 1
  fi
  created_products=$((created_products + 1))

  sku_count=$(rand_between "$SKUS_PER_PRODUCT_MIN" "$SKUS_PER_PRODUCT_MAX")
  for ((s = 1; s <= sku_count; s++)); do
    sku="SKU-${serial}-$(printf "%02d" "$s")"
    inv_body=$(jq -n --arg pid "$product_id" --arg sku "$sku" '{productId:$pid, skuCode:$sku}')
    inv_resp=$(api_call "inventory[$i.$s] create" POST \
      "$ADMIN_GATEWAY/admin/api/v1/inventories" "$inv_body" "$AUTH_HEADER")
    inv_id=$(echo "$inv_resp" | jq -r '.inventory.id')

    stock=$(rand_between "$STOCK_MIN" "$STOCK_MAX")
    inc_body=$(jq -n --argjson a "$stock" '{amount:$a}')
    api_call "inventory[$i.$s] increase" POST \
      "$ADMIN_GATEWAY/admin/api/v1/inventories/${inv_id}/increase" "$inc_body" "$AUTH_HEADER" \
      >/dev/null

    created_inventories=$((created_inventories + 1))
    total_stock=$((total_stock + stock))
  done

  printf "    [%3d/%d] %-40s  price=%-7d  skus=%d\n" \
    "$i" "$PRODUCT_COUNT" "$pname" "$price" "$sku_count"
done

echo
echo "==> done"
echo "    products     : $created_products"
echo "    inventories  : $created_inventories"
echo "    total stock  : $total_stock"
echo "    admin login  : $ADMIN_EMAIL / $ADMIN_PASSWORD"
