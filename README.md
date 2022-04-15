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
