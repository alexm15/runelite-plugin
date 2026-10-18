#!/usr/bin/env bash
# Pre-commit checks: the static Plugin Hub and rules checks (scripts/Check.java), then the Gradle build.
# Mirrors scripts/check.ps1, for Linux, macOS, Git Bash and cloud sessions.
#
#   scripts/check.sh              # static checks + ./gradlew build
#   scripts/check.sh --no-build   # static checks only (a few seconds)
#   scripts/check.sh --submit     # also fail on placeholder names (before a Plugin Hub PR)
#
# Both parts always run, so one run reports everything. Exits non-zero if either fails.
set -euo pipefail
cd "$(dirname "$0")/.."

build=1
check_args=()
for arg in "$@"; do
	case "$arg" in
		--no-build) build=0 ;;
		*) check_args+=("$arg") ;;
	esac
done

java=java
if [ -n "${JAVA_HOME:-}" ]; then
	java="$JAVA_HOME/bin/java"
fi

status=0
"$java" scripts/Check.java ${check_args[@]+"${check_args[@]}"} || status=$?

if [ "$build" = 1 ]; then
	echo
	echo "==> ./gradlew build"
	./gradlew build --console=plain || status=1
fi

exit "$status"
