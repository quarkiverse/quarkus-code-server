package io.quarkiverse.code.server.deployment.devservice;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.code-server.devservices")
public interface CodeServerDevServiceBuildTimeConfig {

    /**
     * Whether to enable the code server dev service.
     */
    Optional<Boolean> enabled();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    Optional<Integer> port();

    /**
     * The code-server image to use.
     * Uses linuxserver/code-server by default because it allows configuration by environment variables
     * as opposed requiring mounting files.
     */
    @WithDefault("lscr.io/linuxserver/code-server:latest")
    String imageName();

    /**
     * Indicates if the Code Server instance managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Code Server starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-code-server} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithDefault("false")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-code-server} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Apicurio Registry looks for a container with the
     * {@code quarkus-dev-service-code-server} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-code-server} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Apicurio Registry instances.
     */
    @WithDefault("code-server")
    String serviceName();

    /**
     * Environment variables that are passed to the container.
     */
    Map<String, String> containerEnv();

}
