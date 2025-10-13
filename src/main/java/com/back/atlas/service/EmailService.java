package com.back.atlas.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${SPRING_MAIL_USERNAME}")
    private String email;

    public void sendResetPasswordEmail(String to, String link) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Reset your password");
            helper.setFrom("noreply@atlas.com");
            helper.setReplyTo(email);


            String htmlContent = buildResetPasswordEmail(link);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error while sending reset password email", e);
        }
    }

    private String buildResetPasswordEmail(String resetLink) {
        return """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f9fc; padding: 20px;">
            <table align="center" width="100%%" style="max-width: 600px; background: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                <tr>
                    <td style="background-color: black; color: white; display: flex; align-items: center: justify-content: center; padding: 20px;">
                        <h2>Reset Your Password</h2>
                    </td>
                </tr>
                <tr>
                    <td style="padding: 30px; text-align: center;">
                        <p style="font-size: 16px; color: #333;">We received a request to reset your password.</p>
                        <p style="font-size: 15px; color: #666;">Click the button below to choose a new one:</p>
                        <a href="%s" style="display: inline-block; background-color: black; color: white; padding: 12px 24px; text-decoration: none; font-weight: medium; margin-top: 20px;">Reset Password</a>
                        <p style="margin-top: 25px; font-size: 13px; color: #999;">If you didn’t request this, you can safely ignore this email.</p>
                    </td>
                </tr>
                <tr>
                    <td style="background-color: #f1f5f9; text-align: center; padding: 10px; font-size: 12px; color: #888;">
                        © 2025 Atlas. All rights reserved.
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(resetLink);
    }
}
