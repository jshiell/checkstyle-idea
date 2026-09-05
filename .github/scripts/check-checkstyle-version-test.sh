#!/usr/bin/env bash
# Test harness for check-checkstyle-version.sh. No framework: hand-rolled
# fail()/assert_eq() helpers. Exits non-zero if any assertion fails.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TESTDATA_DIR="${SCRIPT_DIR}/testdata"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
REAL_PROPERTIES="${REPO_ROOT}/src/main/resources/checkstyle-idea.properties"

# shellcheck source=./check-checkstyle-version.sh
source "${SCRIPT_DIR}/check-checkstyle-version.sh"

FAILURES=0

fail() {
    echo "FAIL: $1" >&2
    FAILURES=$((FAILURES + 1))
}

assert_eq() {
    local expected="$1" actual="$2" description="$3"
    if [[ "${expected}" != "${actual}" ]]; then
        fail "${description}: expected [${expected}], got [${actual}]"
    fi
}

assert_fails() {
    local description="$1"
    shift
    if "$@" >/dev/null 2>&1; then
        fail "${description}: expected non-zero exit, got success"
    fi
}

assert_version_gt() {
    local a="$1" b="$2"
    if ! version_gt "${a}" "${b}"; then
        fail "version_gt(${a}, ${b}): expected true"
    fi
}

assert_not_version_gt() {
    local a="$1" b="$2"
    if version_gt "${a}" "${b}"; then
        fail "version_gt(${a}, ${b}): expected false"
    fi
}

# run_check (parse_args + decide_action) is exercised in-process via a fresh
# `bash -c` that sources the script, rather than executing the script file
# directly. This keeps these tests hermetic: the script's actual entry point
# (run when executed directly, see the BASH_SOURCE guard at the bottom of
# check-checkstyle-version.sh) also drives the issue-lifecycle logic, which
# makes real `gh` calls and is intentionally NOT covered by this offline
# suite — see the "Issue lifecycle" section of check-checkstyle-version.sh.
run_check_in_subshell() {
    bash -c 'source "'"${SCRIPT_DIR}"'/check-checkstyle-version.sh"; run_check "$@"' -- "$@"
}

test_parse_versions_reads_supported_list() {
    local actual
    actual="$(parse_versions "${TESTDATA_DIR}/sample.properties" | tr '\n' ',')"
    assert_eq "10.0,10.1,10.2,10.3.4,11.0.1,11.2.0," "${actual}" "parse_versions on sample fixture"
}

test_parse_versions_real_properties_file() {
    local versions count
    versions="$(parse_versions "${REAL_PROPERTIES}")"
    count="$(printf '%s\n' "${versions}" | wc -l | tr -d ' ')"

    if [[ "${count}" -le 30 ]]; then
        fail "parse_versions on real properties file: expected >30 versions, got ${count}"
    fi
    if ! printf '%s\n' "${versions}" | grep -qx '10.3.4'; then
        fail "parse_versions on real properties file: missing 10.3.4"
    fi
    if ! printf '%s\n' "${versions}" | grep -qx '13.11.0'; then
        fail "parse_versions on real properties file: missing 13.11.0"
    fi
    if [[ "$(printf '%s\n' "${versions}" | tr '\n' ',')" == "14.0.0," ]]; then
        fail "parse_versions on real properties file: only got 14.0.0 (likely read bundledVersions instead)"
    fi
    if printf '%s\n' "${versions}" | grep -q -- '-'; then
        fail "parse_versions on real properties file: contains '-' (likely read checkstyle.versions.map raw)"
    fi
}

test_parse_versions_rejects_invalid_token() {
    assert_fails "parse_versions on invalid-version fixture" parse_versions "${TESTDATA_DIR}/invalid-version.properties"
}

test_parse_versions_rejects_empty_result() {
    assert_fails "parse_versions on empty-versions fixture" parse_versions "${TESTDATA_DIR}/empty-versions.properties"
}

test_parse_map_keys_reads_left_hand_sides() {
    local actual
    actual="$(parse_map_keys "${TESTDATA_DIR}/sample.properties" | tr '\n' ',')"
    assert_eq "8.0,8.1,20.0.0," "${actual}" "parse_map_keys on sample fixture"
}

test_parse_map_keys_real_properties_file() {
    local keys
    keys="$(parse_map_keys "${REAL_PROPERTIES}")"
    if ! printf '%s\n' "${keys}" | grep -qx '8.9'; then
        fail "parse_map_keys on real properties file: missing 8.9 (irregular comma spacing)"
    fi
    if ! printf '%s\n' "${keys}" | grep -qx '10.3'; then
        fail "parse_map_keys on real properties file: missing 10.3"
    fi
}

test_version_gt() {
    assert_not_version_gt "14.0.0" "14.0.0"
    assert_not_version_gt "14.0" "14.0.0"
    assert_not_version_gt "14.0.0" "14.0"
    assert_version_gt "10.0" "9.0"
    assert_version_gt "100.0.0" "14.0.0"
    assert_version_gt "14.0.1" "14.0.0"
}

test_version_max() {
    local actual
    actual="$(version_max "10.0" "14.0.0" "9.5.2" "14.0")"
    assert_eq "14.0.0" "${actual}" "version_max picks the highest version"
}

test_current_max_version_sample_fixture() {
    local actual
    actual="$(current_max_version "${TESTDATA_DIR}/sample.properties")"
    assert_eq "20.0.0" "${actual}" "current_max_version on sample fixture (map key exceeds supported max)"
}

test_current_max_version_real_properties_file() {
    local actual
    actual="$(current_max_version "${REAL_PROPERTIES}")"
    assert_eq "14.1.0" "${actual}" "current_max_version on real properties file"
}

test_select_latest_release_tag_picks_max_matching_tag() {
    local actual
    actual="$(printf 'checkstyle-13.9.0\ncheckstyle-14.0.0\nsome-other-tag\ncheckstyle-9.3\n' | select_latest_release_tag)"
    assert_eq "14.0.0" "${actual}" "select_latest_release_tag picks the max checkstyle-X.Y(.Z) tag"
}

test_select_latest_release_tag_fails_on_no_match() {
    assert_fails "select_latest_release_tag with no matching tags" bash -c \
        'source "'"${SCRIPT_DIR}"'/check-checkstyle-version.sh"; printf "not-a-checkstyle-tag\n" | select_latest_release_tag'
}

test_run_check_reports_update_available() {
    local actual
    actual="$(run_check_in_subshell --properties "${REAL_PROPERTIES}" --latest 99.0.0 --dry-run)"
    if [[ "${actual}" != *"Update available"* ]]; then
        fail "run_check with --latest 99.0.0: expected 'Update available' in [${actual}]"
    fi
}

test_run_check_reports_up_to_date() {
    local actual
    actual="$(run_check_in_subshell --properties "${REAL_PROPERTIES}" --latest 14.0.0 --dry-run)"
    if [[ "${actual}" != *"Up to date"* ]]; then
        fail "run_check with --latest 14.0.0: expected 'Up to date' in [${actual}]"
    fi
}

test_run_check_rejects_invalid_latest() {
    assert_fails "run_check --latest ''" run_check_in_subshell --properties "${REAL_PROPERTIES}" --latest '' --dry-run
    assert_fails "run_check --latest null" run_check_in_subshell --properties "${REAL_PROPERTIES}" --latest null --dry-run
}

test_select_bot_issue_matches_app_prefixed_login() {
    local actual
    actual="$(printf '%s' '[{"number":701,"author":{"login":"app/github-actions","is_bot":true}}]' | select_bot_issue)"
    assert_eq "701" "${actual}" "select_bot_issue matches app/github-actions"
}

test_select_bot_issue_matches_bracket_suffixed_login() {
    local actual
    actual="$(printf '%s' '[{"number":42,"author":{"login":"github-actions[bot]","is_bot":true}}]' | select_bot_issue)"
    assert_eq "42" "${actual}" "select_bot_issue matches github-actions[bot]"
}

test_select_bot_issue_ignores_non_bot_author() {
    local actual
    actual="$(printf '%s' '[{"number":5,"author":{"login":"someuser","is_bot":false}}]' | select_bot_issue)"
    assert_eq "" "${actual}" "select_bot_issue ignores non-bot author"
}

test_select_bot_issue_ignores_unrelated_bot() {
    local actual
    actual="$(printf '%s' '[{"number":9,"author":{"login":"dependabot[bot]","is_bot":true}}]' | select_bot_issue)"
    assert_eq "" "${actual}" "select_bot_issue ignores an unrelated bot"
}

test_run_check_treats_map_key_as_handled() {
    local scratch
    scratch="$(mktemp)"
    cp "${REAL_PROPERTIES}" "${scratch}"
    sed -i.bak 's/13\.4\.1 -> 13\.4\.2/13.4.1 -> 13.4.2, 15.0.0 -> 14.0.0/' "${scratch}"

    local actual
    actual="$(run_check_in_subshell --properties "${scratch}" --latest 15.0.0 --dry-run)"
    if [[ "${actual}" != *"Up to date"* ]]; then
        fail "run_check with map key 15.0.0 present and --latest 15.0.0: expected 'Up to date' in [${actual}]"
    fi

    rm -f "${scratch}" "${scratch}.bak"
}

test_parse_versions_reads_supported_list
test_parse_versions_real_properties_file
test_parse_versions_rejects_invalid_token
test_parse_versions_rejects_empty_result
test_parse_map_keys_reads_left_hand_sides
test_parse_map_keys_real_properties_file
test_version_gt
test_version_max
test_current_max_version_sample_fixture
test_current_max_version_real_properties_file
test_select_latest_release_tag_picks_max_matching_tag
test_select_latest_release_tag_fails_on_no_match
test_run_check_reports_update_available
test_run_check_reports_up_to_date
test_run_check_rejects_invalid_latest
test_run_check_treats_map_key_as_handled
test_select_bot_issue_matches_app_prefixed_login
test_select_bot_issue_matches_bracket_suffixed_login
test_select_bot_issue_ignores_non_bot_author
test_select_bot_issue_ignores_unrelated_bot

if [[ "${FAILURES}" -gt 0 ]]; then
    echo "${FAILURES} test(s) failed." >&2
    exit 1
fi

echo "All tests passed."
