package com.trust.auth.infrastructure.adapter.in.web.mapper;

import com.trust.auth.domain.exception.InvalidScopeException;
import com.trust.auth.domain.model.Scope;
import com.trust.auth.domain.model.command.CreateWorkerCommand;
import com.trust.auth.domain.model.result.WorkerCreatedResult;
import com.trust.auth.infrastructure.adapter.in.web.dto.request.CreateWorkerRequest;
import com.trust.auth.infrastructure.adapter.in.web.dto.response.CreateWorkerResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkerWebMapper {

    default CreateWorkerCommand toCommand(String cdaId, CreateWorkerRequest request) {
        return new CreateWorkerCommand(
                cdaId, request.email(), request.name(), request.roleName(), parseScopes(request.scopes()));
    }

    CreateWorkerResponse toResponse(WorkerCreatedResult result);

    default List<Scope> parseScopes(List<String> scopes) {
        return scopes.stream().map(this::parseScope).toList();
    }

    default Scope parseScope(String scope) {
        try {
            return Scope.valueOf(scope.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidScopeException(scope);
        }
    }
}
