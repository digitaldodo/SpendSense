package com.spendsense.api.service.delivery;

import com.spendsense.api.config.SpendSenseProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class FailoverEmailDeliveryProvider implements EmailDeliveryProvider {
    private static final Logger log = LoggerFactory.getLogger(FailoverEmailDeliveryProvider.class);
    private final SpendSenseProperties properties;
    private final ResendEmailDeliveryProvider resendProvider;
    private final SmtpEmailDeliveryProvider smtpProvider;
    private final LogOnlyEmailDeliveryProvider logOnlyProvider;

    public FailoverEmailDeliveryProvider(
            SpendSenseProperties properties,
            ResendEmailDeliveryProvider resendProvider,
            SmtpEmailDeliveryProvider smtpProvider,
            LogOnlyEmailDeliveryProvider logOnlyProvider
    ) {
        this.properties = properties;
        this.resendProvider = resendProvider;
        this.smtpProvider = smtpProvider;
        this.logOnlyProvider = logOnlyProvider;
    }

    @Override
    public String providerName() {
        return "FAILOVER";
    }

    @Override
    public EmailDeliveryResult send(EmailMessage message) {
        List<EmailDeliveryProvider> providers = orderedProviders();
        EmailDeliveryResult lastFailure = null;
        for (EmailDeliveryProvider provider : providers) {
            if (!provider.available()) {
                continue;
            }
            long started = System.nanoTime();
            EmailDeliveryResult result = provider.send(message);
            long latencyMs = Math.max(1, (System.nanoTime() - started) / 1_000_000);
            if (result.delivered()) {
                log.info(
                        "email_provider_success provider={} deliveryKind={} latencyMs={}",
                        provider.providerName(),
                        message.deliveryKind(),
                        latencyMs
                );
                return result;
            }
            lastFailure = result;
            log.warn(
                    "email_provider_failed provider={} deliveryKind={} errorCode={} latencyMs={}",
                    provider.providerName(),
                    message.deliveryKind(),
                    result.errorCode(),
                    latencyMs
            );
        }
        if (lastFailure != null) {
            return lastFailure;
        }
        return EmailDeliveryResult.failed(providerName(), "NO_PROVIDER_AVAILABLE", "No email delivery provider is enabled.");
    }

    public List<String> providerOrder() {
        return orderedProviders().stream().map(EmailDeliveryProvider::providerName).toList();
    }

    private List<EmailDeliveryProvider> orderedProviders() {
        String primary = properties.delivery().email().primaryProvider();
        List<EmailDeliveryProvider> providers = new ArrayList<>(List.of(resendProvider, smtpProvider, logOnlyProvider));
        providers.sort(Comparator.comparingInt(provider -> provider.providerName().equalsIgnoreCase(primary) ? 0 : 1));
        return providers;
    }
}
