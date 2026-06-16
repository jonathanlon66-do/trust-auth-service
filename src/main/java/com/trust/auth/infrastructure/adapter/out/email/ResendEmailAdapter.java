package com.trust.auth.infrastructure.adapter.out.email;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.trust.auth.domain.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResendEmailAdapter implements EmailPort {

    private final Resend resend;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Value("${trust.frontend-url}")
    private String frontendUrl;

    @Override
    public Mono<Void> sendCdaInvitation(String toEmail, String adminName, String cdaName,
                                         String companyCode, String tempPassword) {
        String intro = "Tu centro <strong style=\"color:#0f172a;\">" + cdaName + "</strong> ya está activo en Trust. "
                + "Estás a un paso de gestionar el cumplimiento de tu CDA en un solo lugar.";
        String body = credentialsBlock(intro, companyCode, toEmail, tempPassword)
                + ctaBlock("Al ingresar por primera vez te pediremos cambiar tu contraseña y completar tu perfil.");
        return send(toEmail, "Tu CDA fue activado en Trust — " + cdaName,
                shell("¡Bienvenido, " + adminName + "! 👋", body));
    }

    @Override
    public Mono<Void> sendWorkerInvitation(String toEmail, String workerName, String cdaName,
                                            String companyCode, String tempPassword) {
        String intro = "Fuiste invitado a unirte a <strong style=\"color:#0f172a;\">" + cdaName + "</strong> en Trust. "
                + "Usa estas credenciales para ingresar por primera vez.";
        String body = credentialsBlock(intro, companyCode, toEmail, tempPassword)
                + ctaBlock("Al ingresar por primera vez te pediremos cambiar tu contraseña y completar tu perfil.");
        return send(toEmail, "Fuiste invitado a " + cdaName + " en Trust",
                shell("¡Hola, " + workerName + "! 👋", body));
    }

    @Override
    public Mono<Void> sendCdaAdded(String toEmail, String workerName, String cdaName, String companyCode) {
        String intro = "Ahora tienes acceso a <strong style=\"color:#0f172a;\">" + cdaName + "</strong> en Trust. "
                + "Ingresa con tu cuenta de siempre y selecciona este centro con el código de empresa.";
        String body = """
                <tr><td style="padding:24px 40px 8px 40px;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc; border:1px solid #e2e8f0; border-radius:12px;">
                    <tr><td style="padding:20px 24px;">
                      <p style="margin:0 0 6px 0; font-size:13px; text-transform:uppercase; letter-spacing:1px; color:#94a3b8;">Código de empresa</p>
                      <strong style="font-size:18px; color:#0f172a;">%s</strong>
                    </td></tr>
                  </table>
                </td></tr>
                """.formatted(companyCode)
                + ctaBlock("Usa tu correo y contraseña actuales — no necesitas crear una cuenta nueva.");
        return send(toEmail, "Ahora tienes acceso a " + cdaName + " en Trust",
                shell("¡Hola, " + workerName + "! 👋", "<tr><td style=\"padding:40px 40px 0 40px;\"><p style=\"margin:0; font-size:16px; line-height:1.6; color:#475569;\">"
                        + intro + "</p></td></tr>" + body));
    }

    private Mono<Void> send(String toEmail, String subject, String html) {
        return Mono.fromCallable(() -> {
            log.debug("Enviando email vía Resend a {} (from={})", toEmail, fromEmail);
            var response = resend.emails().send(CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject(subject)
                    .html(html)
                    .build());
            log.debug("Resend aceptó el email, id={}", response.getId());
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private String credentialsBlock(String intro, String companyCode, String email, String tempPassword) {
        return """
                <tr><td style="padding:40px 40px 0 40px;">
                  <p style="margin:0; font-size:16px; line-height:1.6; color:#475569;">%s</p>
                </td></tr>
                <tr><td style="padding:24px 40px 8px 40px;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc; border:1px solid #e2e8f0; border-radius:12px;">
                    <tr><td style="padding:20px 24px;">
                      <p style="margin:0 0 14px 0; font-size:13px; text-transform:uppercase; letter-spacing:1px; color:#94a3b8;">Tus credenciales de acceso</p>
                      <p style="margin:0 0 10px 0; font-size:15px; color:#334155;">Código de empresa<br><strong style="font-size:17px; color:#0f172a;">%s</strong></p>
                      <p style="margin:0 0 10px 0; font-size:15px; color:#334155;">Correo<br><strong style="font-size:17px; color:#0f172a;">%s</strong></p>
                      <p style="margin:0; font-size:15px; color:#334155;">Contraseña temporal<br>
                        <span style="display:inline-block; margin-top:4px; font-family:'Courier New',monospace; font-size:17px; font-weight:700; color:#1d4ed8; background-color:#e0edff; padding:8px 14px; border-radius:8px; letter-spacing:1px;">%s</span>
                      </p>
                    </td></tr>
                  </table>
                </td></tr>
                """.formatted(intro, companyCode, email, tempPassword);
    }

    private String ctaBlock(String note) {
        return """
                <tr><td style="padding:28px 40px; text-align:center;">
                  <a href="%s" style="display:inline-block; background:linear-gradient(135deg,#1d4ed8,#0ea5e9); color:#ffffff; text-decoration:none; font-size:16px; font-weight:600; padding:15px 44px; border-radius:10px; box-shadow:0 4px 14px rgba(29,78,216,0.35);">
                    Ingresar a Trust →
                  </a>
                  <p style="margin:16px 0 0 0; font-size:13px; color:#94a3b8;">%s</p>
                </td></tr>
                """.formatted(frontendUrl, note);
    }

    private String shell(String heading, String bodyRows) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0; background-color:#eef2f7; font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#eef2f7; padding:32px 16px;">
                    <tr><td align="center">
                      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px; width:100%%; background-color:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 4px 24px rgba(15,23,42,0.08);">
                        <tr>
                          <td style="background:linear-gradient(135deg,#1d4ed8,#0ea5e9); padding:36px 40px; text-align:center;">
                            <div style="font-size:26px; font-weight:700; color:#ffffff; letter-spacing:0.5px;">Trust</div>
                            <div style="font-size:13px; color:#dbeafe; margin-top:4px; text-transform:uppercase; letter-spacing:2px;">Cumplimiento para CDAs</div>
                          </td>
                        </tr>
                        <tr><td style="padding:40px 40px 0 40px;"><h1 style="margin:0; font-size:22px; color:#0f172a;">%s</h1></td></tr>
                        %s
                        <tr>
                          <td style="border-top:1px solid #e2e8f0; padding:24px 40px; text-align:center;">
                            <p style="margin:0; font-size:12px; color:#94a3b8; line-height:1.6;">
                              Recibiste este correo de parte de Trust.<br>
                              Si no esperabas este mensaje, puedes ignorarlo.
                            </p>
                          </td>
                        </tr>
                      </table>
                      <p style="margin:20px 0 0 0; font-size:12px; color:#cbd5e1;">© Trust · Cumplimiento técnico-mecánico</p>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(heading, bodyRows);
    }
}
