# jenkinsfile-shellcheck

shellcheck for Jenkinsfile sh block

## How to use

```sh
usage: jenkinsfile-shellcheck [options]
 -h,--help           usage information
 -i,--input <file1,file2,...>   input files (default: Jenkinsfile)
    --no-expand-gstring         do not expand GString values

 shellcheck options can be written after '--'.
 e.g. groovy jenkinsfile-shellcheck.groovy -- -e SC2154
```

## GitHub Actions

### Inputs

#### `github_token`

**Required**. Must be in form of `github_token: ${{ secrets.github_token }}`'.

#### `shellcheck_flags`

Optional. shellcheck options.

#### `level`

Optional. Report level for reviewdog [info,warning,error].
It's same as `-level` flag of reviewdog.
Default is `error`.

#### `reporter`

Reporter of reviewdog command [github-pr-check,github-pr-review].
Default is `github-pr-check`.

#### `tool_name`

Optional. Tool name to use for reviewdog reporter. Useful when running multiple actions with different config.

#### `exclude`

Optional. List of folders and files to exclude from checking.

#### `filter_mode`

Optional. Filtering mode for the reviewdog command [added, diff_context, file, nofilter]. Default is added.

#### `fail_on_error`

Optional. Exit code for reviewdog when errors are found [true, false] Default is false.

#### `reviewdog_flags`

Optional. Additional reviewdog flags.

### Example Usage

```yml
name: Reviewdog
on: [pull_request]

jobs:
  jenkinsfile-shellcheck:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: srz-zumix/jenkinsfile-shellcheck@v1
      with:
        github_token: ${{ secrets.github_token }}
        reporter: github-pr-review
        shellcheck_flags: "--exclude SC2086" # Optional
```
