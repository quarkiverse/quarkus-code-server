# Quarkus Code Server

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.code-server/quarkus-code-server?logo=apache-maven&style=flat-square)](https://search.maven.org/artifact/io.quarkiverse.code-server/quarkus-code-server)

This is an extension that runs code-server as a devservice.

It is for now a proof of concept, and it is not meant to be used in production dev mode.

## How to use it

1. Build the extension: `mvn install`
2. Add it to your Quarkus project: `quarkus ext add io.quarkiverse.code-server:quarkus-code-server:999-SNAPSHOT`
3. Run in dev mode: `quarkus dev`
4. Will print in console the URL to access code-server
    Also available in dev-ui.


## Challenges

- [x] Run code-server as a devservice
- [ ] Add url link in dev ui tile view
- [ ] Embed code-server URL in dev-ui
- [ ] Pre-install extensions
- [ ] Pre-install JDK within the container
- [ ] Custom build of the codeserver image with the above built?
- [x] Use linuxserver/code-server image instead of codeserver/code-server? (config via env instead of file)

...with the above challenges should we consider to use a different approach? (e.g. install codium with the right extensions and just re-use users local environment?)

