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

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo "check-checkstyle-version.sh: main entry point not yet implemented" >&2
    exit 1
fi
