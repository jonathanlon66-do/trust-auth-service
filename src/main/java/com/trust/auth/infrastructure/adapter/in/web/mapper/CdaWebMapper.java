package com.trust.auth.infrastructure.adapter.in.web.mapper;

import com.trust.auth.domain.model.command.CreateCdaCommand;
import com.trust.auth.domain.model.result.CdaActivationResult;
import com.trust.auth.infrastructure.adapter.in.web.dto.request.CreateCdaRequest;
import com.trust.auth.infrastructure.adapter.in.web.dto.response.CreateCdaResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CdaWebMapper {

    CreateCdaCommand toCommand(CreateCdaRequest request);

    CreateCdaResponse toResponse(CdaActivationResult result);
}
