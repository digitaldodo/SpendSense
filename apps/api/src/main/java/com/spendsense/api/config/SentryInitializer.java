package com.spendsense.api.config;

import io.sentry.Sentry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SentryInitializer implements ApplicationRunner {
    private final String dsn;
    private final double tracesSampleRate;
    private final SpendSenseProperties properties;

    public SentryInitializer(
            @Value("${sentry.dsn:}") String dsn,
            @Value("${sentry.traces-sample-rate:0.0}") double tracesSampleRate,
            SpendSenseProperties properties
    ) {
        this.dsn = dsn;
        this.tracesSampleRate = tracesSampleRate;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (dsn == null || dsn.isBlank()) {
            return;
        }

        SpendSenseProperties.Operations operations = properties.operations();
        Sentry.init(options -> {
            options.setDsn(dsn);
            options.setEnvironment(operations.environment());
            options.setRelease(operations.releaseVersion());
            options.setTracesSampleRate(tracesSampleRate);
            options.setSendDefaultPii(false);
        });
    }
}
