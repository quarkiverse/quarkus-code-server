package io.quarkiverse.code.server.deployment.devservice;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;

/**
 * Starts Code Server as dev service if needed.
 */
@BuildSteps(onlyIf = { IsDevelopment.class, DevServicesConfig.Enabled.class })
public class CodeServerProcessor {

    private static final Logger log = Logger.getLogger(CodeServerProcessor.class);

    private static final int CODE_SERVER_PORT = 8443; // inside the container
    static final String CODE_SERVER_URL_CONFIG = "quarkus.code-server.devservices.url";

    static final String FEATURE = "code-server";
    /**
     * Label to add to shared Dev Service for Code Server running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-code-server-registry";

    private static final ContainerLocator CodeServerContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL,
            CODE_SERVER_PORT);

    static volatile RunningDevService devService;
    static volatile CodeServerDevServiceCfg cfg;
    static volatile boolean first = true;

    @BuildStep
    public DevServicesResultBuildItem startApicurioRegistryDevService(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            CodeServerDevServiceBuildTimeConfig codeServerDevServices,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem, DevServicesConfig devServicesConfig) {

        File workspaceDir = curateOutcomeBuildItem.getApplicationModel().getAppArtifact().getWorkspaceModule().getModuleDir();

        log.infof("Starting Code Server Dev Services %s", workspaceDir);

        CodeServerDevServiceCfg configuration = getConfiguration(codeServerDevServices, workspaceDir);

        if (devService != null) {
            boolean restartRequired = !configuration.equals(cfg);
            if (!restartRequired) {
                return devService.toBuildItem();
            }
            shutdownCodeServer();
            cfg = null;
        }
        StartupLogCompressor compressor = new StartupLogCompressor(
                "Code Server Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);

        try {
            devService = startCodeServer(dockerStatusBuildItem, configuration, launchMode,
                    !devServicesSharedNetworkBuildItem.isEmpty(), devServicesConfig.timeout());
            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devService == null) {
            return null;
        }

        cfg = configuration;

        if (devService.isOwner()) {
            log.infof("Dev Services for Code Server started. Code Server is available at %s",
                    devService.getConfig().get(CODE_SERVER_URL_CONFIG));
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (devService != null) {
                        shutdownCodeServer();
                    }
                    first = true;
                    devService = null;
                    cfg = null;
                }
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        return devService.toBuildItem();
    }

    private Map<String, String> getCodeServerURLConfig(String baseUrl) {
        return Map.of(
                CODE_SERVER_URL_CONFIG, baseUrl + "?folder=/home/coder/project");
    }

    private void shutdownCodeServer() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop Code Server", e);
            } finally {
                devService = null;
            }
        }
    }

    private RunningDevService startCodeServer(DockerStatusBuildItem dockerStatusBuildItem,
            CodeServerDevServiceCfg config, LaunchModeBuildItem launchMode,
            boolean useSharedNetwork, Optional<Duration> timeout) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting dev services for Code Server, as it has been disabled in the config.");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn("Docker isn't working, please run Code Server yourself.");
            return null;
        }

        // Starting the code-server
        // Docs: https://coder.com/docs/code-server/latest/install#docker
        return CodeServerContainerLocator.locateContainer(config.serviceName, config.shared, launchMode.getLaunchMode())
                .map(address -> new RunningDevService(FEATURE,
                        address.getId(), null,
                        // address does not have the URL Scheme - just the host:port, so prepend http://
                        getCodeServerURLConfig("http://" + address.getUrl())))
                .orElseGet(() -> {

                    Map<String, String> env = new HashMap<>();
                    env.put("DOCKER_USER", System.getProperty("user.name")); // does this work on podman?
                    //env.put("PUID", "1000");
                    //env.put("GUID", "1000");
                    env.put("TZ", "Etc/UTC");

                    //let user override
                    env.putAll(config.containerEnv);

                    CodeServerContainer container = new CodeServerContainer(
                            DockerImageName.parse(config.imageName),
                            config.fixedExposedPort,
                            launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null,
                            useSharedNetwork);
                    timeout.ifPresent(container::withStartupTimeout);

                    log.info("Starting a Code Server container using " + container.getDockerImageName());

                    if (config.workspaceDir != null) {
                        container.withFileSystemBind(config.workspaceDir.getAbsolutePath(),
                                "/home/coder/project");
                    }

                    container.withEnv(config.containerEnv);
                    container.start();

                    return new RunningDevService(FEATURE, container.getContainerId(),
                            container::close, getCodeServerURLConfig(container.getUrl()));
                });
    }

    private CodeServerDevServiceCfg getConfiguration(CodeServerDevServiceBuildTimeConfig cfg, File workspaceDir) {
        return new CodeServerDevServiceCfg(cfg, workspaceDir);
    }

    private static final class CodeServerDevServiceCfg {
        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;
        private final boolean shared;
        private final String serviceName;
        private final Map<String, String> containerEnv;
        private final File workspaceDir;

        public CodeServerDevServiceCfg(CodeServerDevServiceBuildTimeConfig config, File workspaceDir) {
            this.devServicesEnabled = config.enabled.orElse(true);
            this.imageName = config.imageName;
            this.fixedExposedPort = config.port.orElse(0);
            this.shared = config.shared;
            this.serviceName = config.serviceName;
            this.containerEnv = config.containerEnv;
            this.workspaceDir = workspaceDir;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CodeServerDevServiceCfg that = (CodeServerDevServiceCfg) o;
            return devServicesEnabled == that.devServicesEnabled
                    && Objects.equals(imageName, that.imageName)
                    && Objects.equals(fixedExposedPort, that.fixedExposedPort)
                    && shared == that.shared
                    && Objects.equals(serviceName, that.serviceName)
                    && Objects.equals(containerEnv, that.containerEnv)
                    && Objects.equals(workspaceDir, that.workspaceDir);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, imageName, fixedExposedPort, shared, serviceName, containerEnv,
                    workspaceDir);
        }
    }

    private static final class CodeServerContainer extends GenericContainer<CodeServerContainer> {
        private final int fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        private CodeServerContainer(DockerImageName dockerImageName, int fixedExposedPort, String serviceName,
                boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "kafka");
                return;
            }

            if (fixedExposedPort > 0) {
                addFixedExposedPort(fixedExposedPort, CODE_SERVER_PORT);
            } else {
                addExposedPorts(CODE_SERVER_PORT);
            }
        }

        public String getUrl() {
            return String.format("http://%s:%s", getHostToUse(), getPortToUse());
        }

        private String getHostToUse() {
            return useSharedNetwork ? hostName : getHost();
        }

        private int getPortToUse() {
            return useSharedNetwork ? CODE_SERVER_PORT : getMappedPort(CODE_SERVER_PORT);
        }
    }
}
