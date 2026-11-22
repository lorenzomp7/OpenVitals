#!/usr/bin/env bash
# Delete every GitHub release but the newest ones, with their tags.
#
# Usage: scripts/prune-github-releases.sh
#
# Environment:
#   GH_REPO        owner/repo, defaults to the checkout's origin
#   KEEP_RELEASES  number of releases to keep, newest first; defaults to 3
#   DRY_RUN        true prints deletions, false deletes them; defaults to true
#
# The newest release is the one README's download link serves, so at least one
# is always kept. Order is by creation time: a newer build always has a higher
# version code, and the release run reads the next code from the kept notes.
set -euo pipefail

KEEP="${KEEP_RELEASES:-3}"
DRY_RUN="${DRY_RUN:-true}"

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
    sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'
    exit 0
fi

case "$KEEP" in
    ''|*[!0-9]*|0)
        echo "KEEP_RELEASES must be a positive integer, got: $KEEP" >&2
        exit 1
        ;;
esac

case "$DRY_RUN" in
    true|false) ;;
    *)
        echo "DRY_RUN must be true or false, got: $DRY_RUN" >&2
        exit 1
        ;;
esac

prune_list="$(
    gh release list --limit 1000 --json tagName,createdAt --jq '
        sort_by(.createdAt) | reverse
        | .['"$KEEP"':][]
        | "\(.tagName)\t\(.createdAt)"
    '
)"

if [ -z "$prune_list" ]; then
    echo "No releases to prune; keeping the newest $KEEP."
    exit 0
fi

count=0
while IFS=$'\t' read -r tag date; do
    count=$((count + 1))
    if [ "$DRY_RUN" = "true" ]; then
        echo "Would delete release and tag $tag ($date)"
    else
        echo "Deleting release and tag $tag ($date)"
        gh release delete "$tag" --yes --cleanup-tag
    fi
done <<< "$prune_list"
echo "$count release(s) beyond the newest $KEEP."
