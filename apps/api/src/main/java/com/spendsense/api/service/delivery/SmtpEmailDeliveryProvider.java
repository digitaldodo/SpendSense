package com.spendsense.api.service.delivery;

import com.spendsense.api.config.SpendSenseProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmtpEmailDeliveryProvider implements EmailDeliveryProvider {
    private final SpendSenseProperties properties;
    private final JavaMailSender mailSender;

    public SmtpEmailDeliveryProvider(SpendSenseProperties properties, JavaMailSender mailSender) {
        this.properties = properties;
        this.mailSender = mailSender;
    }

    @Override
    public String providerName() {
        return "SMTP";
    }

    @Override
    public boolean available() {
        return Boolean.TRUE.equals(properties.delivery().email().smtp().enabled());
    }

    @Override
    public EmailDeliveryResult send(EmailMessage message) {
        if (!StringUtils.hasText(message.to())) {
            return EmailDeliveryResult.failed(providerName(), "MISSING_RECIPIENT", "No email recipient is configured.");
        }
        if (!available()) {
            return EmailDeliveryResult.failed(providerName(), "PROVIDER_DISABLED", "SMTP is not enabled.");
        }

        SpendSenseProperties.Smtp smtp = properties.delivery().email().smtp();
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(message.to());
            helper.setFrom(smtp.fromEmail(), smtp.fromName());
            helper.setSubject(message.subject());
            helper.setText(message.textBody(), message.htmlBody());
            mailSender.send(mimeMessage);
            return EmailDeliveryResult.delivered(providerName(), mimeMessage.getMessageID());
        } catch (MessagingException | MailException | UnsupportedEncodingException exception) {
            return EmailDeliveryResult.failed(providerName(), "SMTP_SEND_FAILED", trim(exception.getMessage(), 520));
        }
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
