package com.example.finalproject.service;

import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.domain.GameRoom;
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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QPost.post;
import static com.example.finalproject.domain.QGameRoom.gameRoom;
import static com.example.finalproject.domain.QGameRoomMember.gameRoomMember;
import static com.example.finalproject.domain.QMember.member;

@RequiredArgsConstructor
@Service
public class SearchService {

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

    // 게시글 검색 (타이틀 기준 검색)
    public ResponseEntity<PrivateResponseBody> searchPost(
            HttpServletRequest request,
            StringDto stringDto) {

        // 인증 정보 검증
        authorizeToken(request);

        // 게시글 제목을 기준으로 검색 키워드로 패턴 비교하여 게시글들 조회
        List<Post> searchPost = jpaQueryFactory
                .selectFrom(post)
                .where(post.title.like("%" + stringDto.getValue().replace(" ", "%") + "%"))
                .fetch();

        // 최종적으로 반환될 게시글 리스트
        List<HashMap<String, String>> searchPostList = new ArrayList<>();

        // 검색 결과로 나온 게시글들을 필요한 정보들을 hashmap으로 담는 과정
        for (Post post1 : searchPost) {
            // hashMap으로 저장된 필요한 조회 정보들
            HashMap<String, String> searchPosts = new HashMap<>();

            searchPosts.put("postId", Long.toString(post1.getPostId())); // 게시글 id
            searchPosts.put("title", post1.getTitle()); // 게시글 제목
            searchPosts.put("author", post1.getAuthor()); // 게시글 작성자
            searchPosts.put("viewcnt", Long.toString(post1.getViewcnt())); // 게시글 조회수
            searchPosts.put("createdAt", post1.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))); // 게시글 생성일자

            // 최종 리스트에 저장
            searchPostList.add(searchPosts);
        }

        return new ResponseEntity<>(new PrivateResponseBody(StatusCode.OK, searchPostList), HttpStatus.OK);
    }


    // 게임방 검색 (방이름 기준 검색)
    public ResponseEntity<PrivateResponseBody> searchRoom(
            HttpServletRequest request,
            StringDto stringDto) {

        // 인증 정보 검증
        authorizeToken(request);

        // 게임방 제목을 기준으로 검색 키워드로 패턴 비교하여 게시글들 조회
        List<GameRoom> searchRoom = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomName.like("%" + stringDto.getValue().replace(" ", "%") + "%"))
                .fetch();

        // 최종적으로 반환될 게임방 리스트
        List<HashMap<String, Object>> searchRoomList = new ArrayList<>();

        // 검색 결과로 나온 게임방들의 필요한 정보들을 hashmap으로 담는 과정
        for (GameRoom gameRoom1 : searchRoom) {
            // hashMap으로 저장된 필요한 조회 정보들
            HashMap<String, Object> searchRooms = new HashMap<>();

            // 서브 쿼리를 사용하여 게임방에 참가한 유저들의 정보들을 조회
            List<Member> memberList = jpaQueryFactory
                    .selectFrom(member)
                    .where(member.memberId.eq(jpaQueryFactory // 해당 게임방과 매핑된 gameRoomMember들의 유저 id를 대조하여 member db 정보 조회
                            .select(gameRoomMember.member_id)
                            .from(gameRoomMember)
                            .where(gameRoomMember.gameRoom.eq(gameRoom1))))
                    .fetch();

            ///////////////////
            // 쿼리문 수정 필요///
            ///////////////////


            searchRooms.put("id", gameRoom1.getRoomId()); // 게임방 id
            searchRooms.put("roomName", gameRoom1.getRoomName()); // 게임방 제목
            searchRooms.put("roomPassword", gameRoom1.getRoomPassword()); // 게임방 비밀번호
            searchRooms.put("mode", gameRoom1.getMode()); // 게임방 모드
            searchRooms.put("member", memberList); // 게임방 참가 유저들
            searchRooms.put("owner", gameRoom1.getOwner()); // 게임방 방장
            searchRooms.put("status", gameRoom1.getStatus()); // 게임방 상태

            // 최종 리스트에 저장
            searchRoomList.add(searchRooms);
        }

        return new ResponseEntity<>(new PrivateResponseBody(StatusCode.OK, searchRoomList), HttpStatus.OK);
    }

}
