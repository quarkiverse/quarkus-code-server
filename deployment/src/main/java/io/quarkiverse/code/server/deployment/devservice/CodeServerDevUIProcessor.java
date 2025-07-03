package io.quarkiverse.code.server.deployment.devservice;

import java.util.List;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * Starts Code Server as dev service if needed.
 */
@BuildSteps(onlyIf = { IsDevelopment.class, DevServicesConfig.Enabled.class })
public class CodeServerDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages(List<DevServiceDescriptionBuildItem> devServiceDescriptions) {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        devServiceDescriptions.stream().filter(d -> CodeServerProcessor.FEATURE.equals(d.getName())).forEach(devs -> {

            String url = devs.getConfigs().get(CodeServerProcessor.CODE_SERVER_URL_CONFIG);
            if (url != null) {
                cardPageBuildItem.addPage(Page.externalPageBuilder("IDE")
                        .url(url)
                        .isHtmlContent()
                        .icon("font-awesome-solid:file-lines"));

                cardPageBuildItem.addPage(Page.externalPageBuilder("IDE (External)")
                        .url(url)
                        .doNotEmbed());
            }
        });

        return cardPageBuildItem;
    }
}
