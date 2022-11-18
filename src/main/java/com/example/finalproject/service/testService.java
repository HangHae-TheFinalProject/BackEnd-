package com.example.finalproject.service;

import com.example.finalproject.domain.Member;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class testService {

    private final TokenProvider jwtTokenProvider;
//    @Transactional
    public ResponseEntity<PrivateResponseBody> testrequest(HttpServletRequest request) {

        System.out.println("here2");
        Member member = validateMember(request);
        System.out.println("here3");
        if (null == member) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.DUPLICATED_PASSWORD, null), HttpStatus.BAD_REQUEST);
        }

        System.out.println(member.getNickname());
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, "성공"), HttpStatus.OK);
    }

    @Transactional
    public Member validateMember(HttpServletRequest request) {
        if (!jwtTokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            return null;
        }
        return jwtTokenProvider.getMemberFromAuthentication();
    }
}
