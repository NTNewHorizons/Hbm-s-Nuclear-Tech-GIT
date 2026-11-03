#!/usr/bin/env bash
# Based on GTNH-Actions-Workflows' runServer smoke test and test_no_error_reports.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

timeout_seconds=${SERVER_SMOKE_TIMEOUT:-90}
if [[ ! "$timeout_seconds" =~ ^[1-9][0-9]*$ ]]; then
    echo 'SERVER_SMOKE_TIMEOUT must be a positive integer (seconds).' >&2
    exit 1
fi

mkdir -p build/server-smoke
# A fresh directory prevents old worlds, configs or crash reports affecting the result.
run_dir=$(mktemp -d build/server-smoke/run.XXXXXX)
server_log="$run_dir/server.log"
printf 'eula=true\n' > "$run_dir/eula.txt"
printf 'level-seed=-6202107849386030209\nonline-mode=true\nserver-ip=127.0.0.1\nserver-port=0\n' > "$run_dir/server.properties"
printf 'stop\n' > "$run_dir/stop.txt"

# Forge reads the queued command after startup. Bound both startup and shutdown.
status=0
timeout --kill-after=15s "${timeout_seconds}s" ./gradlew --console=plain --stacktrace \
    "-PrunServerWorkingDirectory=$run_dir" runServer \
    < "$run_dir/stop.txt" 2>&1 | tee "$server_log" || status=$?

failed=0
if (( status != 0 )); then
    echo "Server run failed or timed out (exit $status)." >&2
    failed=1
fi

shopt -s nullglob
crash_reports=("$run_dir"/crash-reports/*.txt)
if (( ${#crash_reports[@]} )); then
    echo 'Server generated crash reports:' >&2
    cat "${crash_reports[@]}" >&2
    failed=1
fi

if grep -Fq \
    -e 'Fatal errors were detected' \
    -e 'The state engine was in incorrect state ERRORED and forced into state SERVER_STOPPED' \
    -e 'Exception stopping the server' "$server_log"; then
    echo 'Server reported a fatal startup or shutdown error.' >&2
    failed=1
fi

if ! grep -Eq 'Done \(.+\)! For help, type "help" or "\?"' "$server_log"; then
    echo 'Server did not finish startup.' >&2
    failed=1
fi

if (( failed )); then
    echo "Server smoke test failed. Logs: $run_dir" >&2
    exit 1
fi

echo "Server smoke test passed. Logs: $run_dir"
