package com.example.finalproject.jwt;

import com.example.finalproject.controller.response.ResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthenticationEntryPointException implements
        AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
                       AuthenticationException authException) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().println(
            new ObjectMapper().writeValueAsString(
                    ResponseDto.fail("BAD_REQUEST", "로그인이 필요합니다.")
            )
    );
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }
}