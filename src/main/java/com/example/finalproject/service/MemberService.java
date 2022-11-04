package com.example.finalproject.service;

import com.example.finalproject.controller.request.MemberRequestDto;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.repository.MemberRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.example.finalproject.domain.QMember.member;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final JPAQueryFactory jpaQueryFactory;
    private final MemberRepository memberRepository;

    public ResponseEntity<PrivateResponseBody> signup(MemberRequestDto memberRequestDto){

        jpaQueryFactory.selectFrom(member)
                .fetch();

        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK,"회원가입 성공"), HttpStatus.OK);
    }

}
