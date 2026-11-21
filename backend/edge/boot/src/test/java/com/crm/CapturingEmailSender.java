package com.crm;

import com.crm.common.application.email.EmailMessage;
import com.crm.common.application.email.EmailSenderService;
import com.crm.common.application.exceptions.ExternalServiceException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** Guarda os e-mails em vez de enviá-los, com a thread que os enviou; pode ser posto para falhar. */
final class CapturingEmailSender implements EmailSenderService {

    private static final Pattern LINK_TOKEN = Pattern.compile("token=([A-Za-z0-9_%\\-]+)");

    record Sent(EmailMessage message, String thread) {
    }

    private final List<Sent> sent = new CopyOnWriteArrayList<>();
    private final AtomicInteger attempts = new AtomicInteger();
    private volatile boolean failing;

    @Override
    public void send(EmailMessage message) {
        attempts.incrementAndGet();

        if (failing)
            throw new ExternalServiceException("servidor de e-mail fora do ar");

        sent.add(new Sent(message, Thread.currentThread().getName()));
    }

    List<Sent> sentTo(String email) {
        return sent.stream().filter(item -> item.message().to().equals(email)).toList();
    }

    /** O token do link do último e-mail enviado a {@code email}, esperando o envio assíncrono. */
    String linkTokenSentTo(String email) {
        Await.until(() -> !sentTo(email).isEmpty());

        var message = sentTo(email).getLast().message();
        var matcher = LINK_TOKEN.matcher(message.text());

        assertThat(matcher.find()).as("link com token no e-mail").isTrue();

        return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
    }

    int attempts() {
        return attempts.get();
    }

    void failing(boolean value) {
        failing = value;
    }

    void reset() {
        sent.clear();
        attempts.set(0);
        failing = false;
    }
}
