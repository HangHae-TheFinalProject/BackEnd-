package com.example.finalproject.service;

import com.example.finalproject.controller.request.CommentRequestDto;
import com.example.finalproject.controller.response.CommentResponseDto;
import com.example.finalproject.controller.response.ResponseDto;
import com.example.finalproject.domain.Comment;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.Post;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.CommentRepository;
import com.example.finalproject.repository.PostRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import java.time.format.DateTimeFormatter;

import static com.example.finalproject.domain.QComment.comment;
import static com.example.finalproject.domain.QPost.post;

@RequiredArgsConstructor
@Service
public class CommentService {
    private final TokenProvider tokenProvider;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
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

    // 댓글 작성
    @Transactional
    public ResponseEntity<?> writeComment(Long postid, CommentRequestDto requestDto, HttpServletRequest request) {

        // 유저 정보와 댓글 쓰려는 post 정보를 불러옴
        Member member = authorizeToken(request);
        Post post = postRepository.findById(postid).orElse(null);

        if (null == post) {
            throw new PrivateException(StatusCode.POST_ERROR);
        }

        // 댓글 생성
        Comment comment = Comment.builder()
                .content(requestDto.getContent())
                .author(member.getNickname())
                .member(member)
                .post(post)
                .build();

        // 댓글을 DB에 저장
        commentRepository.save(comment);
        // post Entity에 있는 Comments리스트에 넣어줌 (게시글 상세보기시 db에서 불러오지 않기 위해)
        post.getComments().add(comment);

        // 댓글 반환하기 위한 Response Dto
        CommentResponseDto commentResponseDto = CommentResponseDto.builder()
                .commentid(comment.getCommentId())
                .content(comment.getContent())
                .author(comment.getAuthor())
                .createdAt(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm")))
                .modifiedAt(comment.getModifiedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm")))
                .build();
        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, commentResponseDto), HttpStatus.OK);
    }


    // 댓글 수정
    @Transactional
    public ResponseEntity<?> updateComment(Long commentid, CommentRequestDto requestDto, HttpServletRequest request) {

        // 유저 정보와 수정하려는 comment 정보를 불러옴
        Member member = authorizeToken(request);

        Comment update_comment = jpaQueryFactory
                .selectFrom(comment)
                .where(comment.commentId.eq(commentid).and(comment.member.eq(member)))
                .fetchOne();

        if (update_comment == null) {
            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.COMMENT_ERROR,null),HttpStatus.BAD_REQUEST);
        }

        // 수정된 내용으로 update
        jpaQueryFactory
                .update(comment)
                .set(comment.content, requestDto.getContent())
                .where(comment.commentId.eq(commentid))
                .execute();

        em.flush();
        em.clear();

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "수정 완료"), HttpStatus.OK);
    }


    @Transactional
    public ResponseEntity<?> deleteComment(Long commentid, HttpServletRequest request) {

        // 유저 정보 불러옴
        Member member = authorizeToken(request);

        // 삭제하려는 comment 불러옴 (삭제하려는 comment가 해당 유저가 쓴게 맞아야 함)
        Comment del_comment = jpaQueryFactory
                .selectFrom(comment)
                .where(comment.commentId.eq(commentid).and(comment.member.eq(member)))
                .fetchOne();

        if (del_comment == null) {
            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.COMMENT_ERROR,null),HttpStatus.BAD_REQUEST);
        }

        // 삭제
        jpaQueryFactory
                .delete(comment)
                .where(comment.commentId.eq(commentid))
                .execute();

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "삭제 완료!"), HttpStatus.OK);
    }

}
