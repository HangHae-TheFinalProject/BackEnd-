package com.example.finalproject.jwt;

import com.example.finalproject.controller.response.ResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class AccessDeniedHandlerException implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().println(
                new ObjectMapper().writeValueAsString(
                        ResponseDto.fail("BAD_REQUEST", "로그인이 필요합니다.")
                )
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        log.info("회원관리 기능 절차(jwt) -> AccessDeniedHandlerException - handle 메소드 (request : {}, response : {}, accessDeniedException : {})", request, response, accessDeniedException.getMessage());
    }
}