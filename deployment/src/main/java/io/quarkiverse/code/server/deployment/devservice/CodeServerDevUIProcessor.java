package io.quarkiverse.code.server.deployment.devservice;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * Starts Code Server as dev service if needed.
 */
@BuildSteps(onlyIf = { IsDevelopment.class, GlobalDevServicesConfig.Enabled.class })
public class CodeServerDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages() {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        String url = "http://localhost:8080";

        cardPageBuildItem.addPage(Page.externalPageBuilder("IDE")
                .url(url)
                .isHtmlContent()
                .icon("font-awesome-solid:file-lines"));

        cardPageBuildItem.addPage(Page.externalPageBuilder("IDE (External)")
                .url(url)
                .doNotEmbed());

        return cardPageBuildItem;
    }
}
