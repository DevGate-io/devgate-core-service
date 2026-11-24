#!/usr/bin/env bash
#
# migrate-legacy-data.sh — one-off copy of the data of the former devgate-user-service,
# devgate-catalog-service and devgate-scaffolder-service databases into devgate_core.
#
# The schema of devgate_core is NOT created here: devgate-core-service must have started once
# against it, so that Liquibase applied db/changelog/baseline/* (the unmodified changeSets of the
# three services — the tables are identical). This script only moves rows:
#
#   1. preflight — every source DB is reachable, the target schema exists, every source table
#      exists in the target, and ALL target tables are empty (a re-run is refused);
#   2. pg_dump --data-only of the three sources (Liquibase bookkeeping tables excluded) into a
#      private temp dir, then ONE `psql --single-transaction` on the target: either every row
#      lands or nothing does. FK constraints stay enabled (no --disable-triggers);
#   3. per-table row counts, source vs target — any mismatch is reported and exits 1.
#
# The source databases are only read, never modified or dropped.
#
# Usage: migrate-legacy-data.sh [--k8s|--docker] [--dry-run]
#   --k8s      all databases live in one Postgres pod (namespace devgate, pod postgres-0)
#   --docker   each database lives in its own container (the per-service docker-compose files)
#   --dry-run  preflight + source row counts only, nothing is written
#
# Env overrides (defaults in brackets):
#   PG_USER [postgres]
#   USERS_DB [users]  CATALOG_DB [devgate_catalog]  SCAFFOLDER_DB [devgate_scaffolder]
#   CORE_DB [devgate_core]
#   k8s:    K8S_NAMESPACE [devgate]  K8S_PG_POD [postgres-0]
#   docker: USERS_CONTAINER [devgate-user-service-db-1]  CATALOG_CONTAINER [devgate-catalog-db]
#           SCAFFOLDER_CONTAINER [devgate-scaffolder-db]  CORE_CONTAINER [devgate-core-db]

set -euo pipefail

err() {
	printf 'Error: %s\n' "$*" >&2
	exit 1
}

usage() {
	sed -n '2,/^$/p' "$0" | sed 's/^# \{0,1\}//'
}

MODE=""
DRY_RUN=0
for arg in "$@"; do
	case "$arg" in
		--k8s) MODE="k8s" ;;
		--docker) MODE="docker" ;;
		--dry-run) DRY_RUN=1 ;;
		-h | --help)
			usage
			exit 0
			;;
		*) err "Unknown option: $arg (see --help)" ;;
	esac
done
[ -n "$MODE" ] || err "Specify --k8s or --docker (see --help)"

PG_USER="${PG_USER:-postgres}"
USERS_DB="${USERS_DB:-users}"
CATALOG_DB="${CATALOG_DB:-devgate_catalog}"
SCAFFOLDER_DB="${SCAFFOLDER_DB:-devgate_scaffolder}"
CORE_DB="${CORE_DB:-devgate_core}"

K8S_NAMESPACE="${K8S_NAMESPACE:-devgate}"
K8S_PG_POD="${K8S_PG_POD:-postgres-0}"

USERS_CONTAINER="${USERS_CONTAINER:-devgate-user-service-db-1}"
CATALOG_CONTAINER="${CATALOG_CONTAINER:-devgate-catalog-db}"
SCAFFOLDER_CONTAINER="${SCAFFOLDER_CONTAINER:-devgate-scaffolder-db}"
CORE_CONTAINER="${CORE_CONTAINER:-devgate-core-db}"

# Runs a command inside the Postgres container that hosts the given location (stdin is passed
# through). In k8s mode every database is on the same pod, the location is ignored. psql/pg_dump
# connect over the local socket, which the official postgres image trusts — no password needed.
run_in() {
	local location="$1"
	shift
	case "$MODE" in
		k8s) kubectl -n "$K8S_NAMESPACE" exec -i "$K8S_PG_POD" -- "$@" ;;
		docker) docker exec -i "$location" "$@" ;;
	esac
}

query() {
	local location="$1" db="$2" sql="$3"
	run_in "$location" psql -U "$PG_USER" -d "$db" -v ON_ERROR_STOP=1 -tA -F '|' -c "$sql"
}

# Exact row count of every application table of a database, as "table|count" lines.
COUNT_SQL="SELECT table_name,
       (xpath('/row/c/text()',
              query_to_xml(format('SELECT count(*) AS c FROM %I.%I', table_schema, table_name),
                           false, true, '')))[1]::text
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
  AND table_name NOT IN ('databasechangelog', 'databasechangeloglock')
ORDER BY table_name;"

count_rows() {
	query "$1" "$2" "$COUNT_SQL"
}

case "$MODE" in
	k8s)
		command -v kubectl >/dev/null 2>&1 || err "kubectl is required for --k8s"
		kubectl -n "$K8S_NAMESPACE" get pod "$K8S_PG_POD" >/dev/null 2>&1 \
			|| err "Pod $K8S_PG_POD not found in namespace $K8S_NAMESPACE"
		;;
	docker)
		command -v docker >/dev/null 2>&1 || err "docker is required for --docker"
		;;
esac

SOURCES=(
	"$USERS_CONTAINER|$USERS_DB"
	"$CATALOG_CONTAINER|$CATALOG_DB"
	"$SCAFFOLDER_CONTAINER|$SCAFFOLDER_DB"
)

printf 'Mode: %s   target: %s\n\n' "$MODE" "$CORE_DB"

# ─── preflight ────────────────────────────────────────────────────────────────

target_counts="$(count_rows "$CORE_CONTAINER" "$CORE_DB")" \
	|| err "Cannot read target database $CORE_DB (does it exist? is ${CORE_CONTAINER} running?)"

for table in users teams projects services templates; do
	printf '%s\n' "$target_counts" | grep -q "^${table}|" \
		|| err "Table '$table' is missing in $CORE_DB — start devgate-core-service once so that Liquibase creates the schema"
done

non_empty="$(printf '%s\n' "$target_counts" | awk -F'|' '$2 != "0" { print "  " $1 ": " $2 }')"
if [ -n "$non_empty" ]; then
	err "$(printf 'Target %s is not empty — refusing to copy (already migrated, or someone registered on the new service):\n%s' "$CORE_DB" "$non_empty")"
fi

declare -A SOURCE_COUNTS=()
for source in "${SOURCES[@]}"; do
	location="${source%%|*}"
	db="${source#*|}"
	counts="$(count_rows "$location" "$db")" || err "Cannot read source database $db (${location})"

	while IFS='|' read -r table rows; do
		[ -n "$table" ] || continue
		printf '%s\n' "$target_counts" | grep -q "^${table}|" \
			|| err "Source table $db.$table does not exist in $CORE_DB — schema mismatch"
		[ -z "${SOURCE_COUNTS[$table]:-}" ] || err "Table '$table' exists in more than one source database"
		SOURCE_COUNTS[$table]="$rows"
	done <<<"$counts"
done

printf 'Source rows:\n'
for table in $(printf '%s\n' "${!SOURCE_COUNTS[@]}" | sort); do
	printf '  %-24s %s\n' "$table" "${SOURCE_COUNTS[$table]}"
done
echo

if [ "$DRY_RUN" -eq 1 ]; then
	echo "Dry run: preflight passed, nothing was written."
	exit 0
fi

# ─── copy ─────────────────────────────────────────────────────────────────────

# Dumps contain password hashes and refresh tokens: keep them in a private dir, always removed.
WORK_DIR="$(mktemp -d)"
chmod 700 "$WORK_DIR"
trap 'rm -rf "$WORK_DIR"' EXIT

for source in "${SOURCES[@]}"; do
	location="${source%%|*}"
	db="${source#*|}"
	printf '>>> pg_dump %s\n' "$db"
	# Each dump is checked on its own: a dump that fails half-way must never reach psql,
	# which would otherwise commit the truncated stream.
	run_in "$location" pg_dump -U "$PG_USER" -d "$db" \
		--data-only --no-owner --no-privileges --schema=public \
		--exclude-table=public.databasechangelog \
		--exclude-table=public.databasechangeloglock \
		>"$WORK_DIR/$db.sql" \
		|| err "pg_dump of $db failed — nothing was written to $CORE_DB"
done

printf '>>> loading into %s (single transaction)\n' "$CORE_DB"
cat "$WORK_DIR/$USERS_DB.sql" "$WORK_DIR/$CATALOG_DB.sql" "$WORK_DIR/$SCAFFOLDER_DB.sql" \
	| run_in "$CORE_CONTAINER" psql -U "$PG_USER" -d "$CORE_DB" -v ON_ERROR_STOP=1 --single-transaction -q -o /dev/null \
	|| err "Load failed and was rolled back — $CORE_DB is unchanged"

# ─── verify ───────────────────────────────────────────────────────────────────

echo
printf '%-24s %10s %10s\n' "table" "source" "target"
mismatch=0
while IFS='|' read -r table rows; do
	[ -n "$table" ] || continue
	expected="${SOURCE_COUNTS[$table]:-0}"
	marker=""
	if [ "$expected" != "$rows" ]; then
		marker="  <-- MISMATCH"
		mismatch=1
	fi
	printf '%-24s %10s %10s%s\n' "$table" "$expected" "$rows" "$marker"
done <<<"$(count_rows "$CORE_CONTAINER" "$CORE_DB")"

echo
if [ "$mismatch" -ne 0 ]; then
	err "Row counts differ between the sources and $CORE_DB"
fi
echo "✅ Data migrated into $CORE_DB, row counts match. Source databases were not modified."
