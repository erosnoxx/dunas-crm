package com.crm.iam.infrastructure.listeners;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.crm.common.application.email.EmailMessage;
import com.crm.common.application.email.EmailSenderService;
import com.crm.common.application.exceptions.ExternalServiceException;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.events.OneTimeTokenIssued;
import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.vos.UserName;
import com.crm.iam.infrastructure.email.IamEmailTemplates;
import com.crm.shared.domain.vos.contact.Email;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OneTimeTokenIssuedListener")
class OneTimeTokenIssuedListenerTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private static final TokenSecret SECRET = TokenSecret.of("AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde");

    private final User user = User.invite(UserName.of("Ana Souza"), Email.of("ana@dunas.com"), Role.COMMERCIAL, NOW);

    private UserRepository userRepository;
    private EmailSenderService sender;
    private OneTimeTokenIssuedListener listener;

    private Logger rootLogger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        sender = mock(EmailSenderService.class);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        listener = new OneTimeTokenIssuedListener(
                userRepository,
                sender,
                new IamEmailTemplates("https://crm.dunas.com.br", Duration.ofHours(48), Duration.ofHours(1))
        );

        // captura tudo, em qualquer nível e de qualquer logger, para provar que o segredo não vai a log
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        originalLevel = rootLogger.getLevel();
        rootLogger.setLevel(Level.ALL);

        appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(appender);
        rootLogger.setLevel(originalLevel);
    }

    private OneTimeTokenIssued event(TokenPurpose purpose) {
        return new OneTimeTokenIssued(UUID.randomUUID(), NOW, user.getId(), purpose, SECRET);
    }

    private EmailMessage sent() {
        var message = ArgumentCaptor.forClass(EmailMessage.class);

        verify(sender).send(message.capture());

        return message.getValue();
    }

    @Test
    @DisplayName("convite: envia ao e-mail do usuário, com o assunto e o link do convite com o segredo")
    void sendsTheInvitation() {
        listener.on(event(TokenPurpose.INVITATION));

        var message = sent();
        var link = "https://crm.dunas.com.br/convite?token=" + SECRET.getValue();

        assertThat(message.to()).isEqualTo("ana@dunas.com");
        assertThat(message.subject()).isEqualTo("Seu convite para o sistema da DUNAS");
        assertThat(message.html()).contains(link);
        assertThat(message.text()).contains(link);
    }

    @Test
    @DisplayName("redefinição: envia o link de redefinição com o segredo")
    void sendsThePasswordReset() {
        listener.on(event(TokenPurpose.PASSWORD_RESET));

        var message = sent();
        var link = "https://crm.dunas.com.br/redefinir-senha?token=" + SECRET.getValue();

        assertThat(message.subject()).isEqualTo("Redefinição de senha na DUNAS");
        assertThat(message.html()).contains(link);
        assertThat(message.text()).contains(link);
    }

    @Test
    @DisplayName("nada é logado com o segredo, nem no envio que dá certo")
    void successfulSendLogsNoSecret() {
        listener.on(event(TokenPurpose.INVITATION));

        assertThat(appender.list).noneSatisfy(this::mentionsSecret);
    }

    @Test
    @DisplayName("falha do envio não propaga, é logada sem o segredo nem o corpo do e-mail")
    void sendFailureIsSwallowedAndLoggedWithoutTheSecret() {
        doThrow(new ExternalServiceException("falha ao enviar e-mail")).when(sender).send(any());

        assertThatCode(() -> listener.on(event(TokenPurpose.INVITATION))).doesNotThrowAnyException();

        assertThat(appender.list)
                .anySatisfy(logged -> assertThat(logged.getLevel()).isEqualTo(Level.ERROR));
        assertThat(appender.list).noneSatisfy(this::mentionsSecret);
        assertThat(appender.list).noneSatisfy(logged -> assertThat(logged.getFormattedMessage()).contains("Olá, Ana"));
    }

    @Test
    @DisplayName("usuário que não existe mais não envia nada e não propaga")
    void missingUserSendsNothing() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThatCode(() -> listener.on(event(TokenPurpose.INVITATION))).doesNotThrowAnyException();

        verify(sender, never()).send(any());
        assertThat(appender.list).noneSatisfy(this::mentionsSecret);
    }

    private void mentionsSecret(ILoggingEvent logged) {
        assertThat(logged.getFormattedMessage()).contains(SECRET.getValue());
    }
}
