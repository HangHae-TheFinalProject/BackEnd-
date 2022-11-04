package com.example.finalproject.service;

import com.example.finalproject.controller.request.MemberRequestDto;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;

//    public ResponseEntity<PrivateResponseBody> signup(MemberRequestDto memberRequestDto){
//
//
//
//        return new ResponseEntity<>(new PrivateResponseBody());
//    }

}
