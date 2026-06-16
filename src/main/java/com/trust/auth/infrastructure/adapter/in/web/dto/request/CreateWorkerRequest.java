package com.trust.auth.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateWorkerRequest(

        @NotBlank(message = "El email del trabajador es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "El nombre del trabajador es obligatorio")
        String name,

        @NotBlank(message = "El nombre del rol es obligatorio")
        String roleName,

        @NotEmpty(message = "Debes asignar al menos un scope")
        List<String> scopes
) {}
