#
# Makefile

defaut: help

help: ## Display this help screen
	@grep -E '^[a-zA-Z][a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sed -e 's/^GNUmakefile://' | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

run:
	groovy jenkinsfile-shellcheck.groovy -- -f gcc

test:
	git ls-files --exclude='*Jenkinsfile*' --exclude='!*.groovy' --ignored --cached | \
        paste -d , -s - | \
		xargs -I{} groovy jenkinsfile-shellcheck.groovy -i {} -- -f gcc

docker-build:
	docker build -t jenkinsfile-shellcheck .

docker-run:
	docker run --rm -it -w /work -v "${PWD}:/work" jenkinsfile-shellcheck

docker-login:
	docker run --rm -it -w /work -v "${PWD}:/work" --entrypoint=bash jenkinsfile-shellcheck
