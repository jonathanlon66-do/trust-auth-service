package com.trust.auth.infrastructure.adapter.out.email;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.trust.auth.domain.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class ResendEmailAdapter implements EmailPort {

    private final Resend resend;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Override
    public Mono<Void> sendCdaInvitation(String toEmail, String adminName, String cdaName,
                                         String companyCode, String tempPassword) {
        return Mono.fromCallable(() -> {
            String html = buildInvitationHtml(adminName, cdaName, companyCode, toEmail, tempPassword);

            resend.emails().send(CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject("Tu CDA fue activado en Trust — " + cdaName)
                    .html(html)
                    .build());
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private String buildInvitationHtml(String adminName, String cdaName, String companyCode,
                                        String email, String tempPassword) {
        return """
                <h2>Bienvenido a Trust, %s</h2>
                <p>Tu CDA <strong>%s</strong> ha sido activado.</p>
                <p>Ingresa con las siguientes credenciales:</p>
                <ul>
                    <li><strong>Código de empresa:</strong> %s</li>
                    <li><strong>Correo:</strong> %s</li>
                    <li><strong>Contraseña temporal:</strong> %s</li>
                </ul>
                <p>Al ingresar por primera vez se te pedirá cambiar tu contraseña y completar tu perfil.</p>
                """.formatted(adminName, cdaName, companyCode, email, tempPassword);
    }
}
