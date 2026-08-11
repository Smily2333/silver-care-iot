#!/usr/bin/env bash
set -euo pipefail

environment_file="${SILVER_CARE_ENV_FILE:-/etc/silver-care-iot/backend.env}"
database_name="${SILVER_CARE_DATABASE_NAME:-silver_care}"

set -a
# shellcheck disable=SC1090
source "$environment_file"
set +a

database_user="${SILVER_CARE_PROD_DB_USERNAME:-${SILVER_CARE_DB_USERNAME:-}}"
database_password="${SILVER_CARE_PROD_DB_PASSWORD:-${SILVER_CARE_DB_PASSWORD:-}}"
if [[ -z "$database_user" || -z "$database_password" ]]; then
  echo "Database credentials are missing from $environment_file" >&2
  exit 1
fi

export MYSQL_PWD="$database_password"
exec mysql --host=127.0.0.1 --protocol=TCP --user="$database_user" --database="$database_name" "$@"
