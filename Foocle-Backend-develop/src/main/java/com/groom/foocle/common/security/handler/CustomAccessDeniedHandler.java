package com.groom.foocle.common.security.handler;

import com.groom.foocle.apiPayload.code.status.ErrorStatus;
import com.groom.foocle.common.security.JwtAuthenticationFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

@Slf4j

public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.error("[ACCESS DENIED]");

        JwtAuthenticationFilter.setErrorResponse(response, ErrorStatus.JWT_FORBIDDEN); //권한이 없음.
    }
}
