package com.example.finalproject.service;

import com.example.finalproject.controller.response.PostResponseDto;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.Post;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QPost.post;

@RequiredArgsConstructor
@Service
public class MyInfoService {

    private final TokenProvider tokenProvider;
    private final JPAQueryFactory jpaQueryFactory;


    // 인증 정보 검증 부분을 한 곳으로 모아놓음
    public Member authorizeToken(HttpServletRequest request) {

        // Access 토큰 유효성 확인
        if (request.getHeader("Authorization") == null) {
            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
        }

        // Refresh 토큰 유요성 확인
        if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
        }

        // Access, Refresh 토큰 유효성 검증이 완료되었을 경우 인증된 유저 정보 저장
        Member member = tokenProvider.getMemberFromAuthentication();

        // 인증된 유저 정보 반환
        return member;
    }


    // 작성한 게시글들 조회
    public ResponseEntity<?> getMyPosts(HttpServletRequest request){

        // 인증된 유저 정보
        Member auth_member = authorizeToken(request);

        List<Post> posts = jpaQueryFactory
                .selectFrom(post)
                .where(post.member.eq(auth_member))
                .fetch();

        HashMap<String, Object> postlist = new HashMap<>();
        List<HashMap<String, Object>> postlistset = new ArrayList<>();

        for(Post post : posts){
            postlist.put("postId",post.getPostId());
            postlist.put("author",post.getAuthor());
            postlist.put("title",post.getTitle());

            postlistset.add(postlist);
        }

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, postlistset), HttpStatus.OK);
    }

    // 작성한 댓글들 조회
//    public ResponseEntity<?> getMyComments(HttpServletRequest request){
//
//        // 인증된 유저 정보
//        Member auth_member = authorizeToken(request);
//
//        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, ""), HttpStatus.OK);
//    }


    // 모든 전적 조회
    public ResponseEntity<PrivateResponseBody> getMyAllRecord(HttpServletRequest request){
        // 인증된 유저 정보
        Member auth_member = authorizeToken(request);

        // 전적 저장
        HashMap<String, Long> allRecordSet = new HashMap<>();
        allRecordSet.put("allPlayCnt",auth_member.getWinNum()+auth_member.getLossNum()); // 총 플레이 수
        allRecordSet.put("winNum", auth_member.getWinNum()); // 전체 승리 수
        allRecordSet.put("lossNum", auth_member.getLossNum()); // 전체 패배 수
        allRecordSet.put("winLIER", auth_member.getWinLIER()); // 라이어로 승리한 수
        allRecordSet.put("loseLIER", auth_member.getLossLIER()); // 라이어로 패배한 수
        allRecordSet.put("winCITIZEN", auth_member.getWinCITIZEN()); //시민으로 승리한 수
        allRecordSet.put("loseCITIZEN", auth_member.getLossCITIZEN()); // 시민으로 패배한 수

        return new ResponseEntity<>(new PrivateResponseBody(StatusCode.OK, allRecordSet), HttpStatus.OK);
    }


    // 회원 정보 조회
    public ResponseEntity<PrivateResponseBody> getMyPersonalInfo(HttpServletRequest request){
        // 인증된 유저 정보
        Member auth_member = authorizeToken(request);

        // 개인정보 저장
        HashMap<String, String> personalInfo = new HashMap<>();
        personalInfo.put("email", auth_member.getEmail()); // 로그인 이메일
        personalInfo.put("nickname", auth_member.getNickname()); // 닉네임

        return new ResponseEntity<>(new PrivateResponseBody(StatusCode.OK, personalInfo), HttpStatus.OK);
    }


//    public ResponseEntity<PrivateResponseBody> getMyReward(HttpServletRequest request){
//        // 인증된 유저 정보
//        Member auth_member = authorizeToken(request);
//
//
//
//        return new ResponseEntity<>(new PrivateResponseBody(StatusCode.OK, ""), HttpStatus.OK);
//    }
}
