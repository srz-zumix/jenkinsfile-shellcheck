#!/bin/bash

# shellcheck disable=SC2086

cd "${GITHUB_WORKSPACE}" || exit

TEMP_PATH="$(mktemp -d)"
PATH="${TEMP_PATH}:$PATH"

REVIEWDOG_VERSION=v0.14.0

echo '::group::🐶 Installing reviewdog ... https://github.com/reviewdog/reviewdog'
curl -sfL https://raw.githubusercontent.com/reviewdog/reviewdog/master/install.sh | sh -s -- -b "${TEMP_PATH}" "${REVIEWDOG_VERSION}" 2>&1
echo '::endgroup::'

echo '::group::🐶 Installing shellchck ...'
apk add shellcheck
echo '::endgroup::'

export REVIEWDOG_GITHUB_API_TOKEN="${INPUT_GITHUB_TOKEN}"

EXCLUDES=""
for exclude_path in $INPUT_EXCLUDE; do
  EXCLUDES="$EXCLUDES --exclude='!$exclude_path'"
done

echo '::group:: Running jenkinsfile-shellcheck with reviewdog 🐶 ...'
git ls-files --exclude='*Jenkinsfile*' --exclude='!*.groovy' --ignored --cached ${EXCLUDES} \
  | xargs -I {} groovy /jenkinsfile-shellchck.groovy -i {} -- ${INPUT_SHELLCHECK_FLAGS} \
  | reviewdog -efm="%f:%l: %m" \
    -name="${INPUT_TOOL_NAME}" \
    -reporter="${INPUT_REPORTER}" \
    -filter-mode="${INPUT_FILTER_MODE}" \
    -fail-on-error="${INPUT_FAIL_ON_ERROR}" \
    -level="${INPUT_LEVEL}" \
    ${INPUT_REVIEWDOG_FLAGS}
EXIT_CODE=$?
echo '::endgroup::'

exit $EXIT_CODE