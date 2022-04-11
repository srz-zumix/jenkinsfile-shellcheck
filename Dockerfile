FROM groovy:alpine

USER root
RUN apk update && \
    apk upgrade && \
    apk add --no-cache -q -f bash curl
COPY entrypoint.sh /entrypoint.sh
COPY jenkinsfile-shellcheck.groovy /jenkinsfile-shellcheck.groovy

ENTRYPOINT [ "/entrypoint.sh" ]
