#!/usr/bin/env bash
# Checks whether checkstyle/checkstyle has published a release newer than
# everything src/main/resources/checkstyle-idea.properties currently knows
# about (via checkstyle.versions.supported or as a checkstyle.versions.map
# key), and files/updates/closes a single tracking GitHub issue accordingly.
set -euo pipefail

VERSION_RE='^[0-9]+\.[0-9]+(\.[0-9]+)?$'

# Joins backslash-continued lines in <file> into single logical lines, then
# extracts the value of the anchored property <name> (everything after the
# first '='), and echoes one trimmed, non-empty comma-separated token per
# line. Mirrors VersionListReader.java's trim().split("\s*,\s*") behaviour.
parse_property_values() {
    local file="$1" name="$2"
    local joined
    joined="$(awk '{ if (sub(/\\[[:space:]]*$/, "")) { printf "%s", $0 } else { print $0 } }' "${file}")"

    local line
    line="$(printf '%s\n' "${joined}" | grep -E "^${name}[[:space:]]*=")"
    if [[ -z "${line}" ]]; then
        echo "check-checkstyle-version.sh: property '${name}' not found in ${file}" >&2
        return 1
    fi

    local value="${line#*=}"
    local IFS=','
    read -ra tokens <<< "${value}"
    for token in "${tokens[@]}"; do
        # trim leading/trailing whitespace
        token="${token#"${token%%[![:space:]]*}"}"
        token="${token%"${token##*[![:space:]]}"}"
        [[ -n "${token}" ]] && printf '%s\n' "${token}"
    done
}

# Parses checkstyle.versions.supported from <file>, validating every token
# against VERSION_RE. Hard-fails on any invalid token or zero parsed tokens.
parse_versions() {
    local file="$1"
    local versions
    versions="$(parse_property_values "${file}" 'checkstyle\.versions\.supported')" || return 1

    if [[ -z "${versions}" ]]; then
        echo "check-checkstyle-version.sh: no versions parsed from checkstyle.versions.supported in ${file}" >&2
        return 1
    fi

    while IFS= read -r version; do
        if [[ ! "${version}" =~ ${VERSION_RE} ]]; then
            echo "check-checkstyle-version.sh: invalid version '${version}' in checkstyle.versions.supported" >&2
            return 1
        fi
    done <<< "${versions}"

    printf '%s\n' "${versions}"
}

# Parses the left-hand sides of checkstyle.versions.map from <file> (i.e.
# the unsupported versions that are deliberately mapped onto a supported
# alternative), validating every key against VERSION_RE. Hard-fails on any
# invalid key.
parse_map_keys() {
    local file="$1"
    local mappings
    mappings="$(parse_property_values "${file}" 'checkstyle\.versions\.map')" || return 1

    while IFS= read -r mapping; do
        [[ -z "${mapping}" ]] && continue
        local key="${mapping%%->*}"
        key="${key#"${key%%[![:space:]]*}"}"
        key="${key%"${key##*[![:space:]]}"}"
        if [[ ! "${key}" =~ ${VERSION_RE} ]]; then
            echo "check-checkstyle-version.sh: invalid map key '${key}' in checkstyle.versions.map" >&2
            return 1
        fi
        printf '%s\n' "${key}"
    done <<< "${mappings}"
}

# Splits version <v> on '.' and echoes exactly three numeric components,
# padding any missing trailing components with 0 (so "14.0" and "14.0.0"
# compare equal). Do not use `sort -V` for version comparison: it treats
# "14.0" and "14.0.0" as different versions, and macOS's BSD `sort` does not
# even support -V. This mirrors the padding semantics of
# src/main/java/org/infernus/idea/checkstyle/VersionComparator.java, which
# treats a missing micro component as 0.
version_components() {
    local v="$1"
    local IFS='.'
    read -ra parts <<< "${v}"
    printf '%d %d %d\n' "${parts[0]:-0}" "${parts[1]:-0}" "${parts[2]:-0}"
}

# True if version <a> is strictly greater than version <b>.
version_gt() {
    local a_major a_minor a_micro b_major b_minor b_micro
    read -r a_major a_minor a_micro <<< "$(version_components "$1")"
    read -r b_major b_minor b_micro <<< "$(version_components "$2")"

    if (( a_major != b_major )); then
        (( a_major > b_major ))
    elif (( a_minor != b_minor )); then
        (( a_minor > b_minor ))
    else
        (( a_micro > b_micro ))
    fi
}

# Echoes the greatest of the given versions.
version_max() {
    local max="$1"
    shift
    for v in "$@"; do
        if version_gt "${v}" "${max}"; then
            max="${v}"
        fi
    done
    printf '%s\n' "${max}"
}

# The highest version <file> already "handles": the max of everything in
# checkstyle.versions.supported plus every checkstyle.versions.map key (a
# map key is a deliberately handled, unsupported version mapped onto a
# supported alternative, so it counts as handled for alerting purposes).
current_max_version() {
    local file="$1"
    local versions
    versions="$(parse_versions "${file}")" || return 1
    local map_keys
    map_keys="$(parse_map_keys "${file}")" || return 1

    local all=()
    while IFS= read -r v; do all+=("${v}"); done <<< "${versions}"
    while IFS= read -r v; do [[ -n "${v}" ]] && all+=("${v}"); done <<< "${map_keys}"

    version_max "${all[@]}"
}

# Reads release tag names (one per line) from stdin, filters to those
# matching checkstyle-X.Y(.Z), and echoes the highest version among them
# (prefix stripped). Hard-fails if none match.
select_latest_release_tag() {
    local tag version
    local versions=()
    while IFS= read -r tag; do
        if [[ "${tag}" =~ ^checkstyle-([0-9]+\.[0-9]+(\.[0-9]+)?)$ ]]; then
            versions+=("${BASH_REMATCH[1]}")
        fi
    done

    if [[ "${#versions[@]}" -eq 0 ]]; then
        echo "check-checkstyle-version.sh: no release tags matched 'checkstyle-X.Y(.Z)'" >&2
        return 1
    fi

    version_max "${versions[@]}"
}

# Fetches the latest non-draft, non-prerelease Checkstyle release version
# from GitHub. Uses the releases list (not /latest), since /latest can be
# masked by an old-line backport release published more recently.
fetch_latest_version() {
    gh api 'repos/checkstyle/checkstyle/releases?per_page=50' \
        --jq '.[] | select(.draft==false and .prerelease==false) | .tag_name' \
        | select_latest_release_tag
}

DEFAULT_PROPERTIES_FILE="$(cd "${SCRIPT_DIR:-$(dirname "${BASH_SOURCE[0]}")}/../.." && pwd)/src/main/resources/checkstyle-idea.properties"

# Parses CLI flags into the globals PROPERTIES_FILE, LATEST_OVERRIDE and
# DRY_RUN. --latest is validated against VERSION_RE immediately (so e.g.
# `--latest ''` or `--latest null` are rejected here, not later).
parse_args() {
    PROPERTIES_FILE="${DEFAULT_PROPERTIES_FILE}"
    LATEST_OVERRIDE=""
    DRY_RUN=false

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --properties)
                PROPERTIES_FILE="$2"
                shift 2
                ;;
            --latest)
                LATEST_OVERRIDE="$2"
                shift 2
                if [[ ! "${LATEST_OVERRIDE}" =~ ${VERSION_RE} ]]; then
                    echo "check-checkstyle-version.sh: --latest value '${LATEST_OVERRIDE}' is not a valid version" >&2
                    return 1
                fi
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            *)
                echo "check-checkstyle-version.sh: unknown argument '$1'" >&2
                return 1
                ;;
        esac
    done
}

# Echoes the version to compare against: the --latest override if given,
# otherwise the actual latest release fetched from GitHub.
resolve_latest_version() {
    if [[ -n "${LATEST_OVERRIDE}" ]]; then
        printf '%s\n' "${LATEST_OVERRIDE}"
    else
        fetch_latest_version
    fi
}

# Prints a human-readable verdict and returns 1 if <latest> is newer than
# <current>, 0 if <current> is already up to date.
decide_action() {
    local current="$1" latest="$2"
    if version_gt "${latest}" "${current}"; then
        echo "Update available: Checkstyle ${latest} has been released (current max known: ${current})"
        return 1
    else
        echo "Up to date: ${current} is already the latest known Checkstyle version"
        return 0
    fi
}

# Parses args, computes the current max and latest versions, and prints the
# verdict. Always returns 0 (an "update available" outcome is not a script
# error).
run_check() {
    parse_args "$@" || return 1

    local current latest
    current="$(current_max_version "${PROPERTIES_FILE}")" || return 1
    latest="$(resolve_latest_version)" || return 1

    decide_action "${current}" "${latest}" || true
    return 0
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    run_check "$@"
fi
