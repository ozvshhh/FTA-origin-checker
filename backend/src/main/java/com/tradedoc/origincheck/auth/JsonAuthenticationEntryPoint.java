package com.tradedoc.origincheck.auth;

import com.tradedoc.origincheck.common.ApiException;
import com.tradedoc.origincheck.common.ErrorCode;
import com.tradedoc.origincheck.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 인증이 필요한 경로에 토큰이 없거나 틀리면 ErrorResponse 로 401(또는 503) 응답한다. */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        ApiException error = request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE) instanceof ApiException e
                ? e
                : new ApiException(ErrorCode.UNAUTHORIZED, "Authorization: Bearer <token> 헤더가 필요합니다.");
        response.setStatus(error.code().status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(error.code(), error.getMessage(), request.getRequestURI()));
    }
}
