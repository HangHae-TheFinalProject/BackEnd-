package com.example.finalproject.service;

import com.example.finalproject.controller.request.PostRequestDto;
import com.example.finalproject.controller.response.PostResponseDto;
import com.example.finalproject.domain.Media;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.Post;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.PostRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QPost.post;
import static com.example.finalproject.domain.QMedia.media;

@RequiredArgsConstructor
@Service
public class PostService {

    private final TokenProvider tokenProvider;
    private final PostRepository postRepository;
    private final ImageUpload imageUpload;
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

    // 게시글 작성
    public ResponseEntity<PrivateResponseBody> writePost(
            HttpServletRequest request,
            PostRequestDto postRequestDto,
            List<MultipartFile> multipartFiles) {

        // 인증 정보 검증을 마친 유저 정보
        Member member = authorizeToken(request);

        // 이미지 파일들을 담기 위한 리스트
        List<Media> medias = null;

        // 업로드할 이미지 파일이 존재할 경우
        if (multipartFiles != null) {
            // 이미지 업로드 인터페이스
//            medias = imageUpload.filesUpload(multipartFiles, post);
        }

        // 게시글 작성
        Post post = Post.builder()
                .title(postRequestDto.getTitle()) // 게시글 제목
                .content(postRequestDto.getContent()) // 게시글 내용
                .author(member.getNickname()) // 게시글을 작성한 유저의 닉네임
                .member(member) // 게시글을 작성한 유저 객
                .medias(medias)
                .build();

        postRepository.save(post);


        PostResponseDto postResponseDto = PostResponseDto.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .author(post.getAuthor())
                .medias(post.getMedias())
                .build();

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, postResponseDto), HttpStatus.OK);
    }


    // 게시글 수정
    @Transactional
    public ResponseEntity<PrivateResponseBody> updatePost(
            HttpServletRequest request,
            Long postId,
            List<MultipartFile> multipartFiles,
            PostRequestDto postRequestDto) {

        // 합칠 떄 사용
        Member auth_member = authorizeToken(request);

        Post update_post = jpaQueryFactory
                .selectFrom(post)
                .where(post.postId.eq(postId).and(post.member.eq(auth_member)))
                .fetchOne();

        if (update_post == null) {
            throw new PrivateException(StatusCode.NOT_MATCH_POST);
        }

        List<Media> medias = null;

        if (multipartFiles != null) {
            medias = imageUpload.filesUpload(multipartFiles, update_post);
        }

        jpaQueryFactory
                .update(post)
                .set(post.title, postRequestDto.getTitle())
                .set(post.content, postRequestDto.getContent())
                .set(post.medias, medias)
                .where(post.postId.eq(postId))
                .execute();

        em.flush();
        em.clear();

        PostResponseDto postResponseDto = PostResponseDto.builder()
                .postId(update_post.getPostId())
                .author(update_post.getAuthor())
                .title(update_post.getTitle())
                .content(update_post.getContent())
                .medias(update_post.getMedias())
                .build();

        // 댓글 기능 합쳐지면 댓글도 PostResponseDto 에 넣어 수정된 게시글과 함께 보여줄 것

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, postResponseDto), HttpStatus.OK);
    }


    // 게시글 삭제
    public ResponseEntity<PrivateResponseBody> deletePost(Long postId, HttpServletRequest request) {

        // 합칠 떄 사용
        Member auth_member = authorizeToken(request);

        Post delete_post = jpaQueryFactory
                .selectFrom(post)
                .where(post.postId.eq(postId).and(post.member.eq(auth_member)))
                .fetchOne();

        if (delete_post == null) {
            throw new PrivateException(StatusCode.NOT_MATCH_POST);
        }

        List<Media> delete_medias = jpaQueryFactory
                .selectFrom(media)
                .where(media.post.eq(delete_post))
                .fetch();

        for (Media delete_media : delete_medias) {
            imageUpload.deleteFile(delete_media.getMediaName());

            jpaQueryFactory
                    .delete(media)
                    .where(media.mediaId.eq(delete_media.getMediaId()))
                    .execute();
        }

        jpaQueryFactory
                .delete(post)
                .where(post.postId.eq(delete_post.getPostId()))
                .execute();

        // 댓글 기능 합쳐지면 댓글도 삭제처리

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "게시글 삭제 성공"), HttpStatus.OK);
    }


    // 게시글 상세 조회
    public ResponseEntity<PrivateResponseBody> getPost(HttpServletRequest request, Long postId) {

        authorizeToken(request);

        Post get_post = jpaQueryFactory
                .selectFrom(post)
                .where(post.postId.eq(postId))
                .fetchOne();

        // 댓글 기능 합쳐지면 댓글도 PostResponseDto, Post 엔티티에 추가 후 보완할 것
        PostResponseDto postResponseDto = PostResponseDto.builder()
                .postId(get_post.getPostId())
                .author(get_post.getAuthor())
                .title(get_post.getTitle())
                .content(get_post.getContent())
                .medias(get_post.getMedias())
                .build();

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, postResponseDto), HttpStatus.OK);
    }


    // 게시글 전체 목록 조회
    public ResponseEntity<PrivateResponseBody> getAllPost(HttpServletRequest request) {

        authorizeToken(request);

//        // 페이징 처리 전용 지역 변수
//        int size = 10; // 페이지 안에 존재하는 게시글 수
//        int postInPage = size * pageNum; // 페이징 처리를 위한 변수

        List<Post> postlist = jpaQueryFactory
                .selectFrom(post)
                .orderBy(post.createdAt.desc())
                .fetch();

        List<HashMap<String, Object>> allPostlist = new ArrayList<>();
        HashMap<String, Object> allPosts = new HashMap<>();

        for (Post post : postlist) {
            allPosts.put("postId", post.getPostId());
            allPosts.put("author", post.getAuthor());
            allPosts.put("title", post.getTitle());

            allPostlist.add(allPosts);
        }

//         페이징 처리 전용
//        List<HashMap<String, Object>> pagingAllPostlist = new ArrayList<>();
//
//        // 페이지에 따른 일정한 게시글을 담는다.
//        for(int i = postInPage - 10 ; i < postInPage ; i++){
//            if(i >= postlist.size()){ // 게시글 수 만큼 반복문이 돌았다면 탈출
//                break;
//            }
//            // 페이징 처리용 리스트에 포스트를 담는다.
//            pagingAllPostlist.add(allPostlist.get(i));
//        }
//
//        // 총 페이지 수
//        int pageCnt = (int)postlist.size() / size;
//
//        // 만약, 총 게시글 수에서 size를 나누었을 때 딱 나누어 떨어지지 않고 나머지가 남아있다면 총 페이지 수에 +1
//        if(!(postlist.size() % size == 0)){
//            pageCnt = pageCnt + 1;
//        }
//
//        // 총 페이지 수와 페이징 처리된 게시글들을 같이 저장
//        HashMap<String, Object> pagingResult = new HashMap<>();
//        pagingResult.put("pageCnt", pageCnt); // 총 페이지 수
//        pagingResult.put("pageInPosts", pagingAllPostlist); // 페이지 안에 존재하는 게시글들
//
//         페이징 처리 전용 반환값
//        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, pagingResult), HttpStatus.OK);

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, allPostlist), HttpStatus.OK);
    }

}
