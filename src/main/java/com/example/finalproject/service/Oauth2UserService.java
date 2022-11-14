package com.example.finalproject.service;

import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.UserDetailsImpl;
import com.example.finalproject.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class Oauth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
//    private final BCryptPasswordEncoder bCryptPasswordEncoder;
//    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("getAttributes : {}", oAuth2User.getAttributes());

        String provider = userRequest.getClientRegistration().getClientId();
        String providerId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String loginId = provider + "_" +providerId;
        String password = oAuth2User.getAttribute("password");
//        String password = passwordEncoder.encode("aa");
        String nickname = oAuth2User.getAttribute("name");
        String role = "ROLE_USER";

        Member member = memberRepository.findByEmailAndNickname(email, nickname);

        if(member == null) {

            member = Member.builder()
                    .email(email)
                    .password(password)
                    .nickname(nickname)
                    .provider(provider)
                    .providerId(providerId)
                    .build();

            memberRepository.save(member);
        }

        return new UserDetailsImpl(member, oAuth2User.getAttributes());
    }
}