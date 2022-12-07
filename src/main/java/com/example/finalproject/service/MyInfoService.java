package com.example.finalproject.service;

import com.example.finalproject.controller.response.GameRoomResponseDto;
import com.example.finalproject.controller.response.PostResponseDto;
import com.example.finalproject.controller.response.RewardResponseDto;
import com.example.finalproject.domain.*;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import static com.example.finalproject.domain.QReward.reward;

@Slf4j
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
        if (userActive.getPlayminute() >= 60) {

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
        if (userActive.getPlayhour() == 0L && userActive.getPlayminute() == 0L) {
            totalPlayTime = userActive.getPlayminute() + "분";
        }
        // 플레이한 시간 단위가 분이라면 몇분 플레이했는지 알려주는 문구 저장
        else if (userActive.getPlayhour() == 0L && userActive.getPlayminute() != 0L) {
            totalPlayTime = userActive.getPlayminute() + "분";
        }
        // 플레이한 시간 단위가 시간이라면 몇시간 플레이했는지 알려주는 문구 저장
        else if (userActive.getPlayhour() != 0L && userActive.getPlayminute() == 0L) {
            totalPlayTime = userActive.getPlayhour() + "시간";
        }
        // 플레이한 시간 단위가 시간, 분 모두 존재한다면 몇시간 몇분 플레이했는지 알려주는 문구 저장
        else if (userActive.getPlayhour() != 0L && userActive.getPlayminute() != 0L) {
            totalPlayTime = userActive.getPlayhour() + "시간 " + userActive.getPlayminute() + "분";
        }

        // 업은 업적의 수를 저장하기 위한 변수 (0개 일때도 0개라고 알려주어야 하기 떄문에 변수를 따로 만듬)
        Integer rewardCnt = 0;

        // 얻은 업적이 존재할 경우
        if (jpaQueryFactory
                .selectFrom(memberReward)
                .where(memberReward.member.eq(auth_member))
                .fetch() != null) {

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
    public ResponseEntity<PrivateResponseBody> getMyReward(HttpServletRequest request, Integer pageNum) {
        // 인증된 유저 정보
        Member auth_member = authorizeToken(request);

        // 한 페이지 당 보여지는 방 수 (4개)
        int size = 8;
        // 페이징 처리를 위해 현재 페이지와 보여지는 방 수를 곱해놓는다. (4개의 방 수 중 가장 마지막에 나올 위치값)
        int sizeInPage = pageNum * size;

        // 전체 업적들을 불러오기
        List<Reward> rewardlist = jpaQueryFactory
                .selectFrom(reward)
                .fetch();

        // 최종적으로 반환될 보유 업적들을 DTO 형식으로 저장하기 위해 리스트 생성
        List<RewardResponseDto> rewardlistdtos = new ArrayList<>();

        // 얻은 업적들 불러오기
        List<MemberReward> userrewards = jpaQueryFactory
                .selectFrom(memberReward)
                .where(memberReward.memberid.eq(auth_member.getMemberId()))
                .fetch();

        // 얻은 업적이 존재할 경우
        if (!userrewards.isEmpty()) {

            log.info("획득한 업적이 존재할 경우 진입");

            // 업적들을 하나씩 조회
            for (MemberReward reward1 : userrewards) {

                // 얻은 업적의 DB 정보를 조회
                Reward checkreward = jpaQueryFactory
                        .selectFrom(reward)
                        .where(reward.rewardId.eq(reward1.getRewardid()))
                        .fetchOne();

                // 전체 업적들에서 하나씩 비교
                for(Reward reward2 : rewardlist){
                    // 만약 획득한 업적의 id가 존재할 경우 active를 true로 반영하여 DTO에 저장
                    if(checkreward.getRewardId() == reward2.getRewardId()){
                        rewardlistdtos.add(
                                RewardResponseDto.builder()
                                        .rewardId(checkreward.getRewardId()) // 업적 id
                                        .rewardName(checkreward.getRewardName()) // 업적 이름
                                        .rewardDescription(checkreward.getRewardDescription()) // 업적 조건
                                        .mentation(checkreward.getMentation()) // 업적 문구
                                        .isGold(checkreward.isGold()) // 업적 황금 테두리 여부
                                        .isActive(true) // 업적 획득 여부
                                        .build()
                        );
                    }else if(reward1.getRewardid() != reward2.getRewardId()){
                        // 획득한 업적 이외의 획득하지 못한 업적들은 active 속성을 false로 저장
                        rewardlistdtos.add(
                                RewardResponseDto.builder()
                                        .rewardId(reward2.getRewardId()) // 업적 id
                                        .rewardName(reward2.getRewardName()) // 업적 이름
                                        .rewardDescription(reward2.getRewardDescription()) // 업적 조건
                                        .mentation(reward2.getMentation()) // 업적 문구
                                        .isGold(reward2.isGold()) // 업적 황금 테두리 여부
                                        .isActive(false) // 업적 획득 여부
                                        .build()
                        );
                    }
                }
            }
        }else{
            // 업적을 획득한 적이 없을 경우

            log.info("획득한 업적이 없을 경우 진입");

            // active 속성이 false인 상태로 모든 업적이 DTO에 저장
            for(Reward reward2 : rewardlist){
                    rewardlistdtos.add(
                            RewardResponseDto.builder()
                                    .rewardId(reward2.getRewardId()) // 업적 id
                                    .rewardName(reward2.getRewardName()) // 업적 이름
                                    .rewardDescription(reward2.getRewardDescription()) // 업적 조건
                                    .mentation(reward2.getMentation()) // 업적 문구
                                    .isGold(reward2.isGold()) // 황금 테두리 여부
                                    .isActive(false) // 획득 여부
                                    .build()
                    );
            }
        }

        // 페이징 처리 후 8개의 업적만을 보여줄 리스트
        List<RewardResponseDto> rewardsInPage = new ArrayList<>();

        // 페이징 처리 후 나온 페이지에 존재하는 8개의 업적을 담는다.
        for (int i = sizeInPage - size; i < sizeInPage; i++) {

            // 업적을 담는다.
            rewardsInPage.add(rewardlistdtos.get(i));

            // 지금 존재하는 전체 업적의 개수와 i 값이 같다면 break로 더이상 담지 않고 빠져나온다.
            if (i == rewardlistdtos.size() - 1) {
                break;
            }
        }

//        // 페이지 수
//        int pageCnt = rooms.size() / size;
//
//        // 만약 페이지 수가 size 와 딱 맞아떨어지지 않고 더 많다면 +1을 해준다.
//        if (!(rooms.size() % size == 0)) {
//            pageCnt = pageCnt + 1;
//        }

        // page 번호와 페이지에 존재하는 업적들을 담기위한 hashmap
        HashMap<String, Object> pageRewardSet = new HashMap<>();

//        // 최대 페이지
//        pageRoomSet.put("pageCnt", pageCnt);
        // 페이지 안에 있는 업적들
        pageRewardSet.put("rewardsInPage", rewardsInPage);

        return new ResponseEntity<>(new PrivateResponseBody(StatusCode.OK, pageRewardSet), HttpStatus.OK);
    }
}
