package com.crawljax.core;

import static com.google.common.base.Preconditions.checkNotNull;

import com.crawljax.core.ExitNotifier.ExitStatus;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.InputSpecification;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.di.CoreModule;
import com.crawljax.forms.FormInput;
import com.crawljax.forms.FormInputValueHelper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Runs crawljax given a certain {@link CrawljaxConfiguration}. Run {@link #call()} to start a
 * crawl.
 */
public class CrawljaxRunner implements Callable<CrawlSession> {

    private final CrawljaxConfiguration config;
    private CrawlController controller;
    private ExitStatus reason;

    public CrawljaxRunner(CrawljaxConfiguration config) {
        this.config = config;
        readFormDataFromFile();
    }

    /**
     * Reads input data from a JSON file in the output directory.
     */
    private void readFormDataFromFile() {
        List<FormInput> formInputList = FormInputValueHelper.deserializeFormInputs(config.getSiteDir());

        if (formInputList != null) {
            InputSpecification inputSpecs = config.getCrawlRules().getInputSpecification();

            for (FormInput input : formInputList) {
                inputSpecs.inputField(input);
            }
        }
    }

    /**
     * Runs Crawljax with the given configuration.
     *
     * @return The {@link CrawlSession} once the Crawl is done.
     */
    @Override
    public CrawlSession call() {
        Injector injector = Guice.createInjector(new CoreModule(config));
        controller = injector.getInstance(CrawlController.class);
        CrawlSession session = controller.call();
        reason = controller.getReason();
        return session;
    }

    /**
     * Stops Crawljax. It will try to shutdown gracefully and run the {@link PostCrawlingPlugin}s.
     */
    public void stop() {
        checkNotNull(controller, "Cannot stop Crawljax if you haven't started it");
        controller.stop();
    }

    /**
     * @return The {@link ExitStatus} Crawljax stopped or <code>null</code> if it hasn't stopped yet.
     */
    public ExitStatus getReason() {
        return reason;
    }
}
