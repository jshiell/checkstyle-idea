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

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo "check-checkstyle-version.sh: main entry point not yet implemented" >&2
    exit 1
fi
