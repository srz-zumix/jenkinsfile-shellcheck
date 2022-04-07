#
# Makefile

defaut: help

help: ## Display this help screen
	@grep -E '^[a-zA-Z][a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sed -e 's/^GNUmakefile://' | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

run:
	groovy jenkinsfile-shellcheck.groovy -- -f gcc

test:
	git ls-files --exclude='*Jenkinsfile*' --exclude='!*.groovy' --ignored --cached | \
		xargs -I{} groovy jenkinsfile-shellcheck.groovy -i {} -- -f gcc

docker-build:
	docker build -t jenkinsfile-shellcheck .
