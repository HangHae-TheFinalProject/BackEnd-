package com.example.finalproject.service;

import com.example.finalproject.controller.response.PostResponseDto;
import com.example.finalproject.controller.response.RewardResponseDto;
import com.example.finalproject.domain.*;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QMemberReward.memberReward;
import static com.example.finalproject.domain.QPost.post;
import static com.example.finalproject.domain.QMemberActive.memberActive;

@RequiredArgsConstructor
@Service
public class MyInfoService {

    private final TokenProvider tokenProvider;
    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager em;


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
    public ResponseEntity<?> getMyPosts(HttpServletRequest request) {

        // 인증된 유저 정보
        Member auth_member = authorizeToken(request);

        // 유저가 작성한 게시글들 불러오기
        List<Post> posts = jpaQueryFactory
                .selectFrom(post)
                .where(post.member.eq(auth_member))
                .fetch();

        // 적성한 게시글의 일부 정보들을 hashmap으로 저장
        HashMap<String, Object> postlist = new HashMap<>();
        // hashmap으로 저장한 게시글 일부 정보들을 리스트화하여 저장 (최종적으로 목록이 보여지는 리스트)
        List<HashMap<String, Object>> postlistset = new ArrayList<>();

        // 작성된 게시글들 하나씩 조회
        for (Post post : posts) {
            postlist.put("postId", post.getPostId()); // 게시글 id
            postlist.put("author", post.getAuthor()); // 게시글 작성자
            postlist.put("title", post.getTitle()); // 게시글 제목

            // hashmap 으로 저장된 게시글 정보들을 최종 리스트에 저장
            postlistset.add(postlist);
        }

        // 최종 리스트 반환
        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, postlistset), HttpStatus.OK);
    }


    // 모든 전적 및 닉네임 조회
    @Transactional
    public ResponseEntity<PrivateResponseBody> getMyAllRecord(HttpServletRequest request) {
        // 인증된 유저 정보
        Member auth_member = authorizeToken(request);

        // 해당 유저의 활동이력 정보 불러오기
        MemberActive userActive = jpaQueryFactory
                .selectFrom(memberActive)
                .where(memberActive.member.eq(auth_member))
                .fetchOne();

        // 플레이한 n분이 60분을 초과할 경우
        if(userActive.getPlayminute() >= 60){

            // 60분이 초과되어 시간으로 환산될 변수
            Long overminute = userActive.getPlayminute() / 60;
            // 시간으로 환산된 후 남은 n분
            Long remainminute = userActive.getPlayminute() - (overminute * 60);

            // 유저의 활동이력에서 60분 초과시 플레이 시간, 플레이 n분 업데이트
            jpaQueryFactory
                    .update(memberActive)
                    .set(memberActive.playhour, userActive.getPlayhour() + overminute) // 플레이 시간 (환산된 시간을 더해서 업데이트)
                    .set(memberActive.playminute, remainminute) // 환산되고 남은 n분으로 업데이트
                    .where(memberActive.member.eq(auth_member))
                    .execute();

            em.flush();
            em.clear();
        }

        // view 단에서 보여지게될 플레이시간 문구를 저장하기 위한 변수
        String totalPlayTime = "";

        // 플레이 시간 혹은 플레이 n분이 없다면 아직 플레이하지 않았다는 문구 저장
        if(userActive.getPlayhour() == 0L && userActive.getPlayminute() == 0L){
            totalPlayTime = "아직 플레이하지 않았습니다.";
        }
        // 플레이한 시간 단위가 분이라면 몇분 플레이했는지 알려주는 문구 저장
        else if(userActive.getPlayhour() == 0L && userActive.getPlayminute() != 0L){
            totalPlayTime = userActive.getPlayminute() + "분";
        }
        // 플레이한 시간 단위가 시간이라면 몇시간 플레이했는지 알려주는 문구 저장
        else if(userActive.getPlayhour() != 0L && userActive.getPlayminute() == 0L){
            totalPlayTime = userActive.getPlayhour() + "시간";

        }
        // 플레이한 시간 단위가 시간, 분 모두 존재한다면 몇시간 몇분 플레이했는지 알려주는 문구 저장
        else if(userActive.getPlayhour() != 0L && userActive.getPlayminute() != 0L){
            totalPlayTime = userActive.getPlayhour() + "시간 " + userActive.getPlayminute() + "분";
        }

        // 업은 업적의 수를 저장하기 위한 변수 (0개 일때도 0개라고 알려주어야 하기 떄문에 변수를 따로 만듬)
        Integer rewardCnt = 0;

        // 얻은 업적이 존재할 경우
        if(jpaQueryFactory
                .selectFrom(memberReward)
                .where(memberReward.member.eq(auth_member))
                .fetch() != null){

            // 존재하는 업적의 개수를 변수에 저장
            rewardCnt = jpaQueryFactory
                    .selectFrom(memberReward)
                    .where(memberReward.member.eq(auth_member))
                    .fetch().size();
        }


        // 전적, 업적 개수, 닉네임과 같은 마이페이지에 보여질 정보들을 hashmap으로 저장
        HashMap<String, String> allRecordSet = new HashMap<>();

        allRecordSet.put("nickname", auth_member.getNickname()); // 닉네임
        allRecordSet.put("allPlayRecord", auth_member.getWinNum() + auth_member.getLossNum() + "전 " + auth_member.getWinNum() + "승 " + auth_member.getLossNum() + "패"); // 총 플레이 수
        allRecordSet.put("winLIER", auth_member.getWinLIER() + "승"); // 라이어로 승리한 수
        allRecordSet.put("winCITIZEN", auth_member.getWinCITIZEN() + "승"); //시민으로 승리한 수
        allRecordSet.put("totalPlayTime", totalPlayTime); // 총 플레이 시간
        allRecordSet.put("rewardCnt", rewardCnt + "개"); // 보유 리워드 개수

        return new ResponseEntity<>(new PrivateResponseBody(StatusCode.OK, allRecordSet), HttpStatus.OK);
    }

    // 얻은 업적 조회
    @Transactional
    public ResponseEntity<PrivateResponseBody> getMyReward(HttpServletRequest request) {
        // 인증된 유저 정보
        Member auth_member = authorizeToken(request);

        // 최종적으로 반환될 보유 업적들을 DTO 형식으로 저장하기 위해 리스트 생성
        List<RewardResponseDto> rewardlist = new ArrayList<>();

        // 얻은 업적이 존재할 경우
        if (jpaQueryFactory
                .selectFrom(memberReward)
                .where(memberReward.member.eq(auth_member))
                .fetch() != null) {

            // 얻은 업적들 불러오기
            List<MemberReward> userrewards = jpaQueryFactory
                    .selectFrom(memberReward)
                    .where(memberReward.member.eq(auth_member))
                    .fetch();

            // 업적들을 하나씩 조회
            for (MemberReward reward1 : userrewards) {
                // DTO 타입으로 변환하여 리스트에 저장
                rewardlist.add(
                        RewardResponseDto.builder()
                                .rewardId(reward1.getRewardid()) // 업적 id
                                .rewardName(reward1.getRewardName()) // 업적 이름
                                .build()
                );
            }
        }

        return new ResponseEntity<>(new PrivateResponseBody(StatusCode.OK, rewardlist), HttpStatus.OK);
    }
}
