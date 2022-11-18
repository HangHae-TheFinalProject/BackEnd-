package com.example.finalproject.service;

import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.domain.GameRoomMember;
import com.example.finalproject.domain.GameStartSet;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.repository.GameStartSetRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QGameRoomMember.gameRoomMember;

import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Comparator;

@RequiredArgsConstructor
@Service
public class GameHTTPService {

    private final GameStartSetRepository gameStartSetRepository;
    private final JPAQueryFactory jpaQueryFactory;
    static int cnt =0;
//    static int test =0;
    static HashMap<String, Integer> voteHashMap = new HashMap<>();
    public Boolean isLier(Long gameroomid, StringDto stringDto) {
        String nickname = stringDto.getValue();
        GameStartSet gameStartSet= gameStartSetRepository.findByRoomId(gameroomid);
        return gameStartSet.getLier().equals(nickname);
    }
    public ResponseEntity<PrivateResponseBody> vote(Long gameroomid, StringDto stringDto){
        String name = stringDto.getValue();
        voteHashMap.put(name, voteHashMap.getOrDefault(name, 0) + 1);
        cnt ++;

//        GameStartSet gameStartSet= gameStartSetRepository.findByRoomId(gameroomid);
        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .fetch();
        int memberNum = gameRoomMembers.size();
        if(cnt == memberNum){
            return sortHash();
        }
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, null), HttpStatus.CONTINUE);
    }

    public Boolean isAnswer(Long gameroomid, StringDto stringDto) {
        String answer = stringDto.getValue();
        GameStartSet gameStartSet= gameStartSetRepository.findByRoomId(gameroomid);
        return gameStartSet.getKeyword().equals(answer);
    }

    public ResponseEntity<PrivateResponseBody> sortHash(){
        List<Entry<String, Integer>> list_entries = new ArrayList<Entry<String, Integer>>(voteHashMap.entrySet());

        // 비교함수 Comparator를 사용하여 내림 차순으로 정렬
        Collections.sort(list_entries, new Comparator<Entry<String, Integer>>() {
            // compare로 값을 비교
            public int compare(Entry<String, Integer> obj1, Entry<String, Integer> obj2)
            {
                // 내림 차순으로 정렬
                return obj2.getValue().compareTo(obj1.getValue());
            }
        });
        int maxValue = 0;
        for(Entry<String, Integer> entry : list_entries) {
            maxValue = entry.getValue();
            break;
        }
        List<String> nickName = new ArrayList<>();
        for(Entry<String, Integer> entry : list_entries) {
            if(entry.getValue()==maxValue){
                nickName.add(entry.getKey());
            }
        }
        voteHashMap.clear();
        cnt=0;
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, nickName), HttpStatus.OK);
    }

    public ResponseEntity<PrivateResponseBody> oneMoerRound(Long gameroomid) {
        GameStartSet gameStartSet= gameStartSetRepository.findByRoomId(gameroomid);
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, gameStartSet.oneMoerRound()), HttpStatus.OK);
    }

    public Boolean isRound(Long gameroomid) {
        GameStartSet gameStartSet= gameStartSetRepository.findByRoomId(gameroomid);
        return gameStartSet.getRound()<4;
    }


    ///////////////test
    public Boolean isLiertest(StringDto stringDto) {
        String nickname = stringDto.getValue();
        return "amy".equals(nickname);
    }

    public ResponseEntity<PrivateResponseBody> votetest(int num, StringDto stringDto){
        String name = stringDto.getValue();
        voteHashMap.put(name, voteHashMap.getOrDefault(name, 0) + 1);
        System.out.println(name + " : "+voteHashMap.get(name));
        cnt ++;
        if(cnt == num){
            return sortHash();
        }
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, null), HttpStatus.OK);
    }


//    public void test(){
//        Map<String, Integer> testMap = new HashMap<String, Integer>();
//
//        // Map에 데이터 추가
//        testMap.put( "apple", 1);
//        testMap.put( "orange", 2);
//        testMap.put( "pineapple", 4);
//        testMap.put( "strawberry", 5);
//        testMap.put( "melon", 3);
//
//        // Map.Entry 리스트 작성
//        List<Entry<String, Integer>> list_entries = new ArrayList<Entry<String, Integer>>(testMap.entrySet());
//
//        // 비교함수 Comparator를 사용하여 내림 차순으로 정렬
//        Collections.sort(list_entries, new Comparator<Entry<String, Integer>>() {
//            // compare로 값을 비교
//            public int compare(Entry<String, Integer> obj1, Entry<String, Integer> obj2)
//            {
//                // 내림 차순으로 정렬
//                return obj2.getValue().compareTo(obj1.getValue());
//            }
//        });
//
//        System.out.println("내림 차순 정렬");
//        // 결과 출력
//        for(Entry<String, Integer> entry : list_entries) {
//            System.out.println(entry.getKey() + " : " + entry.getValue());
//        }
//
////        System.out.println(test);
////        test++;
//////        if(test == 2) test =0;
//    }
}
