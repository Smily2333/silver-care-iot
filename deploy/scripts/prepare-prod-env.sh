#!/usr/bin/env bash
set -euo pipefail

source_file="${1:-/etc/silver-care-iot/backend.env}"
target_file="${2:-/etc/silver-care-iot/backend-prod.env}"

if [[ ! -f "$source_file" ]]; then
  echo "Source environment file does not exist: $source_file" >&2
  exit 1
fi
if [[ -e "$target_file" ]]; then
  echo "Refusing to overwrite existing production environment: $target_file" >&2
  exit 1
fi

install -m 600 -o root -g root "$source_file" "$target_file"
sed -i \
  -e 's/^SILVER_CARE_DB_USERNAME=/SILVER_CARE_PROD_DB_USERNAME=/' \
  -e 's/^SILVER_CARE_DB_PASSWORD=/SILVER_CARE_PROD_DB_PASSWORD=/' \
  -e 's/^SILVER_CARE_ADMIN_USERNAME=/SILVER_CARE_PROD_ADMIN_USERNAME=/' \
  -e 's/^SILVER_CARE_ADMIN_PASSWORD=/SILVER_CARE_PROD_ADMIN_PASSWORD=/' \
  -e 's/^WECHAT_MINIAPP_APPID=/WECHAT_MINIAPP_PROD_APPID=/' \
  -e 's/^WECHAT_MINIAPP_APP_SECRET=/WECHAT_MINIAPP_PROD_APP_SECRET=/' \
  "$target_file"

sed -i '1iSPRING_PROFILES_ACTIVE=prod\nSILVER_CARE_PROD_DB_URL=jdbc:mysql://127.0.0.1:3306/silver_care?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai' "$target_file"
printf '%s\n' \
  'SILVER_CARE_PROD_GATEWAY_ENABLED=true' \
  'SILVER_CARE_PROD_GATEWAY_PORT=9001' \
  'SILVER_CARE_PROD_GEOCODING_ENABLED=false' \
  'SILVER_CARE_PROD_CONFIRMED_DEVICE_ACTIONS=' \
  'SILVER_CARE_PROD_ALLOW_HEALTH_WITHOUT_WEAR_STATUS=false' \
  'SILVER_CARE_PROD_AUTOMATIC_MONITORING_ENABLED=false' \
  >> "$target_file"

echo "Created $target_file with production-only variable names; the active service was not changed."
