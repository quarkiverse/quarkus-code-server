package io.quarkiverse.code.server.deployment.devservice;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "quarkus.code-server.devservices", phase = ConfigPhase.BUILD_TIME)
public class CodeServerDevServiceBuildTimeConfig {

    /**
     * Whether to enable the code server dev service.
     */
    @ConfigItem
    public Optional<Boolean> enabled = Optional.empty();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public Optional<Integer> port;

    /**
     * The code-server image to use.
     *
     */
    @ConfigItem(defaultValue = "lscr.io/linuxserver/code-server:latest")
    public String imageName;

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
    @ConfigItem(defaultValue = "false")
    public boolean shared;

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
    @ConfigItem(defaultValue = "code-server")
    public String serviceName;

    /**
     * Environment variables that are passed to the container.
     */
    @ConfigItem
    public Map<String, String> containerEnv;

}
