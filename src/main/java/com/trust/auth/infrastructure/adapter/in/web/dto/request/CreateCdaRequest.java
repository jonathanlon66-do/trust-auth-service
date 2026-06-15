package com.trust.auth.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateCdaRequest(

        @NotBlank(message = "El nombre del CDA es obligatorio")
        String name,

        @NotBlank(message = "El código de empresa es obligatorio")
        @Pattern(regexp = "^[A-Z0-9\\-]{3,20}$", message = "El código de empresa solo puede contener letras mayúsculas, números y guiones (3-20 caracteres)")
        String companyCode,

        @NotBlank(message = "El email del administrador es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String adminEmail,

        @NotBlank(message = "El nombre del administrador es obligatorio")
        String adminName
) {}
