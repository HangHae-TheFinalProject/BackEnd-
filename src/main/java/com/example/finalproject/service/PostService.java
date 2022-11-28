package com.example.finalproject.service;

import com.example.finalproject.controller.request.PostRequestDto;
import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.controller.response.CommentResponseDto;
import com.example.finalproject.controller.response.MediaResponseDto;
import com.example.finalproject.controller.response.PostResponseDto;
import com.example.finalproject.domain.*;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.PostRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QPost.post;
import static com.example.finalproject.domain.QMedia.media;

@Slf4j
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
    @Transactional
    public ResponseEntity<PrivateResponseBody> writePost(
            HttpServletRequest request,
            PostRequestDto postRequestDto,
            List<MultipartFile> multipartFiles) {

        // 인증 정보 검증을 마친 유저 정보
        Member member = authorizeToken(request);

        // 이미지 파일들을 담기 위한 리스트
        List<Media> medias = null;

        // 게시글 작성
        Post writePost = Post.builder()
                .title(postRequestDto.getTitle()) // 게시글 제목
                .content(postRequestDto.getContent()) // 게시글 내용
                .author(member.getNickname()) // 게시글을 작성한 유저의 닉네임
                .member(member) // 게시글을 작성한 유저 객
                .viewcnt(0L)
                .medias(medias)
                .build();

        postRepository.save(writePost);

        List<MediaResponseDto> mediaResponseDtos = new ArrayList<>();

        // 업로드할 이미지 파일이 존재할 경우
        if (multipartFiles != null) {
            // 이미지 업로드 인터페이스를 이용하여 s3와 media entity에 저장하고 이미지들이 담긴 리스트를 반환받는다.
            medias = imageUpload.filesUpload(multipartFiles, writePost);

            // 작성한 게시글의 이미지 파일들을 업데이트함
            jpaQueryFactory
                    .update(post)
                    .set(post.medias, medias)
                    .where(post.postId.eq(writePost.getPostId()))
                    .execute();

            em.flush();
            em.clear();

            for(Media media1 : writePost.getMedias()){
                MediaResponseDto mediaResponseDto = MediaResponseDto.builder()
                        .mediaId(media1.getMediaId())
                        .mediaName(media1.getMediaName())
                        .mediaUrl(media1.getMediaUrl())
                        .build();

                mediaResponseDtos.add(mediaResponseDto);
            }

        }

        // Dto 로 출력할 내용들을 저장
        PostResponseDto postResponseDto = PostResponseDto.builder()
                .postId(writePost.getPostId()) // 작성 게시글 id
                .title(writePost.getTitle()) // 작성 게시글 제목
                .content(writePost.getContent()) // 작성 게시글 내용
                .author(writePost.getAuthor()) // 작성 게시글 작성자
                .viewcnt(writePost.getViewcnt())
                .createdAt(writePost.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm")))
                .modifiedAt(writePost.getModifiedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm")))
                .medias(mediaResponseDtos) // 작성 게시글 이미지 파일들
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

        // 인증 정보를 가진 유저
        Member auth_member = authorizeToken(request);

        // 수정할 게시글 조회
        Post update_post = jpaQueryFactory
                .selectFrom(post)
                .where(post.postId.eq(postId).and(post.member.eq(auth_member)))
                .fetchOne();

        // 수정할 게시글이 존재하지 않는다면 작성자가 작성한 게시글이 아닌 경우라고 판단
        if (update_post == null) {
            throw new PrivateException(StatusCode.NOT_MATCH_POST);
        }

        List<CommentResponseDto> comments = new ArrayList<>();

        if(!update_post.getComments().isEmpty()){
            List<Comment> commentList = update_post.getComments();

            for (Comment existcomment : commentList) {
                comments.add(
                        CommentResponseDto.builder()
                                .commentid(existcomment.getCommentId())
                                .content(existcomment.getContent())
                                .author(existcomment.getAuthor())
                                .build());
            }
        }


        // 수정할 이미지 파일들을 저장할 리스트
        List<Media> medias = null;
        List<MediaResponseDto> mediaResponseDtos = new ArrayList<>();

        // 수정할 이미지 파일들이  존재하고, 현재 등록된 이미지들이 존재할 경우
        if (multipartFiles != null && !update_post.getMedias().isEmpty()) {
            // 이미지 업로드 인터페이스를 이용해 이미지 등록
            medias = imageUpload.filesUpload(multipartFiles, update_post);

            // 수정할 게시글에 속한 이미지 파일들 조회
            List<Media> mediaList = jpaQueryFactory
                    .selectFrom(media)
                    .where(media.post.eq(update_post))
                    .fetch();

            // 기존에 존재했던 이미지들 삭제
            for (Media existMedia : mediaList) {
                // s3에서 삭제
                imageUpload.deleteFile(existMedia.getMediaName());

                // db에서 삭제
                jpaQueryFactory
                        .delete(media)
                        .where(media.post.eq(update_post))
                        .execute();
            }


            for(Media media1 : update_post.getMedias()){
                MediaResponseDto mediaResponseDto = MediaResponseDto.builder()
                        .mediaId(media1.getMediaId())
                        .mediaName(media1.getMediaName())
                        .mediaUrl(media1.getMediaUrl())
                        .build();

                mediaResponseDtos.add(mediaResponseDto);
            }

            // 수정할 게시글 내용과 이미지를 업데이트
            jpaQueryFactory
                    .update(post)
                    .set(post.title, postRequestDto.getTitle())
                    .set(post.content, postRequestDto.getContent())
                    .set(post.medias, medias)
                    .set(post.modifiedAt, LocalDateTime.now())
                    .where(post.postId.eq(postId))
                    .execute();

            em.flush();
            em.clear();

            // <이미지들을 삭제 후 업데이트하는 이유>
            // 예를 들어, 기존에 등록된 이미지가 2장이고 새로이 수정하고자 하는 이미지가 3장일 경우
            // 기등록된 이미지 중 어떠한 이미지가 새로운 이미지 중 하나로 변경되어할지 모르기 때문에
            // 삭제 후 업데이트 하는 형식을 취한 것이다.

        } else if (multipartFiles != null && update_post.getMedias().isEmpty()) { // 수정할 이미지 파일들이  존재하고, 현재 등록된 이미지가 없을 경우
            // 이미지 업로드 인터페이스를 이용해 이미지 등록 및 게시글에 반영
            medias = imageUpload.filesUpload(multipartFiles, update_post);

            // 수정할 게시글 내용과 이미지를 업데이트
            jpaQueryFactory
                    .update(post)
                    .set(post.title, postRequestDto.getTitle())
                    .set(post.content, postRequestDto.getContent())
                    .set(post.medias, medias)
                    .set(post.modifiedAt, LocalDateTime.now())
                    .where(post.postId.eq(postId))
                    .execute();

            em.flush();
            em.clear();

        }else{

            // 수정할 게시글 내용과 이미지를 업데이트
            jpaQueryFactory
                    .update(post)
                    .set(post.title, postRequestDto.getTitle())
                    .set(post.content, postRequestDto.getContent())
                    .set(post.modifiedAt, LocalDateTime.now())
                    .where(post.postId.eq(postId))
                    .execute();

            em.flush();
            em.clear();

        }



        // 수정된 게시글의 정보를 Dto에 저장
        PostResponseDto postResponseDto = PostResponseDto.builder()
                .postId(update_post.getPostId()) // 수정된 게시글 id
                .author(update_post.getAuthor()) // 수정된 게시글의 작성자
                .title(update_post.getTitle()) // 수정된 게시글의 제목
                .content(update_post.getContent()) // 수정된 게시글의 내용
                .viewcnt(update_post.getViewcnt()) // 조회 수
                .createdAt(update_post.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm"))) // 생성일자
                .modifiedAt(update_post.getModifiedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm"))) // 수정일자
                .medias(mediaResponseDtos) // 수정된 게시글의 이미지들
                .comments(comments) // 게시글에 작성된 댓글들
                .build();




        // 댓글 기능 합쳐지면 댓글도 PostResponseDto 에 넣어 수정된 게시글과 함께 보여줄 것

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, postResponseDto), HttpStatus.OK);
    }


    // 게시글 삭제
    @Transactional
    public ResponseEntity<PrivateResponseBody> deletePost(Long postId, HttpServletRequest request) {

        // 인증 정보를 가진 유저 정보
        Member auth_member = authorizeToken(request);

        // 삭제하고자 하는 게시글이 작성자가 작성한 게시글이 맞는지 확인
        Post delete_post = jpaQueryFactory
                .selectFrom(post)
                .where(post.postId.eq(postId).and(post.member.eq(auth_member)))
                .fetchOne();

        // 삭제하고자 하는 게시글이 현재 로그인한 유저의 게시글이 아닐 경우
        if (delete_post == null) {
            throw new PrivateException(StatusCode.NOT_MATCH_POST);
        }

        // s3 에서도 삭제하기 위해 게시글에 저장된 이미지 파일들을 조회
        List<Media> delete_medias = jpaQueryFactory
                .selectFrom(media)
                .where(media.post.eq(delete_post))
                .fetch();

        // 이미지 파일들의 이름을 가지고 s3 에서 삭제처리
        for (Media delete_media : delete_medias) {
            imageUpload.deleteFile(delete_media.getMediaName());
        }

        // media DB에 저장된 이미지들 삭제
        jpaQueryFactory
                .delete(media)
                .where(media.post.eq(delete_post))
                .execute();

        // 해당 게시글 삭제
        jpaQueryFactory
                .delete(post)
                .where(post.postId.eq(delete_post.getPostId()))
                .execute();

        // 댓글 기능 합쳐지면 댓글도 삭제처리

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "게시글 삭제 성공"), HttpStatus.OK);
    }


    // 게시글 상세 조회
    @Transactional
    public ResponseEntity<PrivateResponseBody> getPost(HttpServletRequest request, Long postId) {

        // 인증 정보 유효성 검증
        authorizeToken(request);

        // 상세 조회할 게시글 조회
        Post getPost = jpaQueryFactory
                .selectFrom(post)
                .where(post.postId.eq(postId))
                .fetchOne();

        // 댓글 추가
        List<CommentResponseDto> commentResponseDtoList = new ArrayList<>();
        List<MediaResponseDto> mediaResponseDtos = new ArrayList<>();

        if(!getPost.getComments().isEmpty()){
            for (Comment comment : getPost.getComments()) {
                commentResponseDtoList.add(
                        CommentResponseDto.builder()
                                .commentid(comment.getCommentId())
                                .content(comment.getContent())
                                .author(comment.getAuthor())
                                .createdAt(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm")))
                                .modifiedAt(comment.getModifiedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm")))
                                .build()
                );
            }
        }

        if(!getPost.getMedias().isEmpty()){
            for (Media media1  : getPost.getMedias()) {
                mediaResponseDtos.add(
                        MediaResponseDto.builder()
                                .mediaId(media1.getMediaId())
                                .mediaName(media1.getMediaName())
                                .mediaUrl(media1.getMediaUrl())
                                .build()
                );
            }
        }

        // 조회 수 증가
        jpaQueryFactory
                .update(post)
                .set(post.viewcnt, getPost.getViewcnt() + 1L)
                .where(post.postId.eq(getPost.getPostId()))
                .execute();


        // 상세 조회할 게시글 정보를 Dto에 저장
        PostResponseDto postResponseDto = PostResponseDto.builder()
                .postId(getPost.getPostId()) // 조회할 게시글 id
                .author(getPost.getAuthor()) // 조회할 게시글 작성자
                .title(getPost.getTitle()) // 조회할 게시글 제목
                .content(getPost.getContent()) // 조회할 게시글 내용
                .viewcnt(getPost.getViewcnt()) // 조회 수
                .createdAt(getPost.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm"))) // 생성일자
                .modifiedAt(getPost.getModifiedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm"))) // 수정일자
                .medias(mediaResponseDtos) // 조회할 게시글에 속한 이미지파일들
                .comments(commentResponseDtoList) // 댓글들
                .build();

        em.flush();
        em.clear();

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, postResponseDto), HttpStatus.OK);
    }


    // 게시글 전체 목록 조회
    public ResponseEntity<PrivateResponseBody> getAllPost(HttpServletRequest request, String sort) {

        // 인증 정보 유효성 검증
        authorizeToken(request);

//        // 페이징 처리 전용 지역 변수
//        int size = 10; // 페이지 안에 존재하는 게시글 수
//        int postInPage = size * pageNum; // 페이징 처리를 위한 변수

        // hashmap 으로 저장된 게시글의 내용들을 리스트로 저장
        List<HashMap<String, Object>> allPostlist = new ArrayList<>();

        if(sort.equals("recent")){
            log.info("최신 순 조회 : {}", sort);

            // 최근 생성일자 기준으로 작성된 게시글들 전체 리스트 저장
            List<Post> postlist = jpaQueryFactory
                    .selectFrom(post)
                    .orderBy(post.createdAt.desc())
                    .fetch();

            // 목록 출력 시 필요한 항목들 hashmap에 저장
            for (Post post : postlist) {
                // 목록 조회이기 때문에 Dto 가 아닌 hashmap 으로 일부 보여질 내용들을 저장
                HashMap<String, Object> allPosts = new HashMap<>();

                allPosts.put("postId", post.getPostId()); // 게시글 id
                allPosts.put("author", post.getAuthor()); // 게시글 작성자
                allPosts.put("title", post.getTitle()); // 게시글 제목
                allPosts.put("createdAt", post.getCreatedAt()); // 게시글 생성일자
                allPosts.put("viewcnt", post.getViewcnt()); // 게시글 조회수

                allPostlist.add(allPosts);
            }

        }else if(sort.equals("view")){ //
            log.info("조회 수 조회 : {}", sort);

            // 최근 조회수 기눚으로 작성된 게시글들 전체 리스트 저장
            List<Post> postlist = jpaQueryFactory
                    .selectFrom(post)
                    .orderBy(post.viewcnt.desc())
                    .fetch();

            // 목록 출력 시 필요한 항목들 hashmap에 저장
            for (Post post : postlist) {
                // 목록 조회이기 때문에 Dto 가 아닌 hashmap 으로 일부 보여질 내용들을 저장
                HashMap<String, Object> allPosts = new HashMap<>();

                allPosts.put("postId", post.getPostId()); // 게시글 id
                allPosts.put("author", post.getAuthor()); // 게시글 작성자
                allPosts.put("title", post.getTitle()); // 게시글 제목
                allPosts.put("createdAt", post.getCreatedAt()); // 게시글 생성일자
                allPosts.put("viewcnt", post.getViewcnt()); // 게시글 조회수

                allPostlist.add(allPosts);
            }

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
