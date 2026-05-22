package com.example.notifications.service.delivery;

import com.example.notifications.config.EmailProperties;
import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailProperties emailProperties;
    private EmailSender sender;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        sender = new EmailSender(emailProperties, mailSender);
    }

    @Test
    void send_ShouldSimulateByDefault() {
        Notification notification = notification("student@example.com");

        sender.send(notification);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_ShouldCallYandexSmtpWhenProviderIsYandexSmtp() {
        emailProperties.setProvider(EmailProperties.Provider.YANDEX_SMTP);
        EmailProperties.YandexSmtp yandexSmtp = emailProperties.getYandexSmtp();
        yandexSmtp.setUsername("sender@yandex.ru");
        yandexSmtp.setPassword("application-password");
        yandexSmtp.setFrom("sender@yandex.ru");
        yandexSmtp.setSubject("Test subject");
        Notification notification = notification("student@example.com");

        sender.send(notification);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("sender@yandex.ru");
        assertThat(message.getTo()).containsExactly("student@example.com");
        assertThat(message.getSubject()).isEqualTo("Test subject");
        assertThat(message.getText()).isEqualTo("hello");
    }

    @Test
    void send_ShouldUseUsernameAsFromWhenFromIsBlank() {
        emailProperties.setProvider(EmailProperties.Provider.YANDEX_SMTP);
        EmailProperties.YandexSmtp yandexSmtp = emailProperties.getYandexSmtp();
        yandexSmtp.setUsername("sender@yandex.ru");
        yandexSmtp.setPassword("application-password");
        Notification notification = notification("student@example.com");

        sender.send(notification);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getFrom()).isEqualTo("sender@yandex.ru");
    }

    @Test
    void send_ShouldRejectMissingYandexPassword() {
        emailProperties.setProvider(EmailProperties.Provider.YANDEX_SMTP);
        emailProperties.getYandexSmtp().setUsername("sender@yandex.ru");
        Notification notification = notification("student@example.com");

        assertThatThrownBy(() -> sender.send(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_ShouldRejectMissingDestination() {
        Notification notification = notification("");

        assertThatThrownBy(() -> sender.send(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EMAIL destination");
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_ShouldWrapSmtpErrors() {
        emailProperties.setProvider(EmailProperties.Provider.YANDEX_SMTP);
        EmailProperties.YandexSmtp yandexSmtp = emailProperties.getYandexSmtp();
        yandexSmtp.setUsername("sender@yandex.ru");
        yandexSmtp.setPassword("application-password");
        Notification notification = notification("student@example.com");
        doThrow(new MailSendException("smtp error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> sender.send(notification))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("Yandex SMTP request failed");
    }

    private Notification notification(String destination) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .channel(Channel.EMAIL)
                .destination(destination)
                .payload("hello")
                .status(Status.PENDING)
                .build();
    }
}
