FROM groovy:alpine

USER root
COPY entrypoint.sh /entrypoint.sh
COPY jenkinsfile-shellcheck.groovy /jenkinsfile-shellcheck.groovy

ENTRYPOINT [ "/entrypoint.sh" ]
