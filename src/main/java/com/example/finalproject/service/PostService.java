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
    public ResponseEntity<?> writePost(
            HttpServletRequest request,
            PostRequestDto postRequestDto,
            List<MultipartFile> multipartFiles) {

        // 합칠 떄 사용
        Member member = authorizeToken(request);

        // 테스트 시 활용할 임의 멤버
//        Member member = jpaQueryFactory
//                .selectFrom(QMember.member)
//                .where(QMember.member.id.eq(1L))
//                .fetchOne();
//        if (member == null) {
//            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
//        }

        List<Media> medias = null;

        Post post = Post.builder()
                .title(postRequestDto.getTitle())
                .content(postRequestDto.getContent())
                .author(member.getNickname())
                .member(member)
                .build();

        if (multipartFiles != null) {
            medias = imageUpload.filesUpload(multipartFiles, post);
        }

        post = Post.builder()
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
    public ResponseEntity<?> updatePost(
            HttpServletRequest request,
            Long postId,
            List<MultipartFile> multipartFiles,
            PostRequestDto postRequestDto) {

        // 합칠 떄 사용
        Member auth_member = authorizeToken(request);

        // 테스트 시 활용할 임의 멤버
//        Member member = jpaQueryFactory
//                .selectFrom(QMember.member)
//                .where(QMember.member.id.eq(1L))
//                .fetchOne();
//        if (member == null) {
//            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
//        }

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
    public ResponseEntity<?> deletePost(Long postId, HttpServletRequest request) {

        // 합칠 떄 사용
        Member auth_member = authorizeToken(request);

        // 테스트 시 활용할 임의 멤버
//        Member member = jpaQueryFactory
//                .selectFrom(QMember.member)
//                .where(QMember.member.id.eq(1L))
//                .fetchOne();
//        if (member == null) {
//            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
//        }

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

        for(Media delete_media : delete_medias){
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
    public ResponseEntity<?> getPost(HttpServletRequest request, Long postId){

        // 합칠 떄 사용
        Member auth_member = authorizeToken(request);

        // 테스트 시 활용할 임의 멤버
//        Member member = jpaQueryFactory
//                .selectFrom(QMember.member)
//                .where(QMember.member.id.eq(1L))
//                .fetchOne();
//        if (member == null) {
//            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
//        }

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
    public ResponseEntity<?> getAllPost(HttpServletRequest request, Pageable pageable){

        // 합칠 떄 사용
        Member auth_member = authorizeToken(request);

        // 테스트 시 활용할 임의 멤버
//        Member member = jpaQueryFactory
//                .selectFrom(QMember.member)
//                .where(QMember.member.id.eq(1L))
//                .fetchOne();
//        if (member == null) {
//            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
//        }

        Page<Post> allPosts = postRepository.findAll(pageable);

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, allPosts), HttpStatus.OK);
    }

}
