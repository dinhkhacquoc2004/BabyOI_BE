package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:no-reply@babyoi.local}")
    private String fromEmail;

    @Override
    @Async("applicationTaskExecutor")
    public void sendOtp(String toEmail, String subject, String code, int expiresInMinutes) {
        log.warn("BabyOi OTP for {} is {}. It expires in {} minutes.", toEmail, code, expiresInMinutes);

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            log.warn("Mail sender is not configured.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "BabyOi");
            helper.setTo(toEmail);
            helper.setReplyTo(fromEmail);
            helper.setSubject(subject);
            helper.setText(
                    """
                    Xin chào,

                    Mã xác nhận BabyOi của bạn là: %s

                    Mã này có hiệu lực trong %d phút. Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.

                    BabyOi
                    """.formatted(code, expiresInMinutes),
                    """
                    <div style="font-family:Arial,sans-serif;color:#23313d;line-height:1.6">
                      <h2 style="color:#ff8fab;margin-bottom:8px">Mã xác nhận BabyOi</h2>
                      <p>Xin chào,</p>
                      <p>Mã xác nhận của bạn là:</p>
                      <div style="font-size:28px;font-weight:700;letter-spacing:6px;color:#ff8fab;margin:16px 0">%s</div>
                      <p>Mã này có hiệu lực trong <strong>%d phút</strong>.</p>
                      <p>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.</p>
                      <p style="margin-top:24px">BabyOi</p>
                    </div>
                    """.formatted(code, expiresInMinutes)
            );

            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (Exception exception) {
            log.warn("Could not send OTP email to {}. Dev OTP is {}", toEmail, code, exception);
        }
    }
}
