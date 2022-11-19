package com.example.finalproject.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum StatusCode {
    OK(HttpStatus.OK, "200", "응답이 정상 처리 되었습니다."),
    LOGIN_OK(HttpStatus.OK, "200", "로그인 되셨습니다!"),

    LOGIN_MEMBER_ID_FAIL(HttpStatus.NOT_FOUND, "110", "존재하지 않는 유저 정보입니다."),
    LOGIN_PASSWORD_FAIL(HttpStatus.BAD_REQUEST, "111", "비밀번호가 일치하지 않습니다."),
    LOGIN_WRONG_SIGNATURE_JWT_TOKEN(HttpStatus.BAD_REQUEST, "112", "잘못된 JWT 서명입니다."),
    LOGIN_EXPIRED_JWT_TOKEN(HttpStatus.BAD_REQUEST, "113", "만료된 JWT 토큰입니다."),
    LOGIN_NOT_SUPPORTED_JWT_TOKEN(HttpStatus.BAD_REQUEST, "114", "지원되지 않는 JWT 토큰입니다."),
    LOGIN_WRONG_FORM_JWT_TOKEN(HttpStatus.BAD_REQUEST, "115", "JWT 토큰이 잘못되었습니다."),
    LOGIN_MEMBER_REQUIRED_INFORMATION_FAIL(HttpStatus.BAD_REQUEST, "116", "필수 입력 정보를 입력 후 시도해주세요"),
    NOT_MATCH_POST(HttpStatus.BAD_REQUEST, "117", "현재 로그인한 유저가 작성한 게시글이 아닙니다."),
    DUPLICATED_NICKNAME(HttpStatus.BAD_REQUEST,"117","중복된 email이 있습니다."),
    DUPLICATED_PASSWORD(HttpStatus.BAD_REQUEST,"118","Password가 틀립니다."),
    POST_ERROR(HttpStatus.BAD_REQUEST,"119","게시글 작성이 필요합니다."),
    NOT_EXIST_MEDIA(HttpStatus.BAD_REQUEST,"120","이미지가 존재하지 않아 게시글 작성이 불가합니다."),
    NOT_FOUND_ROOM(HttpStatus.BAD_REQUEST,"121","입장할 방이 존재하지 않습니다."),
    CANT_ENTER(HttpStatus.BAD_REQUEST,"122","입장 정원이 초과하였습니다."),
    MEMBER_DUPLICATED(HttpStatus.BAD_REQUEST,"123","이미 입장한 유저입니다."),
    UNAUTHORIZE(HttpStatus.BAD_REQUEST,"124","방장만이 게임 시작을 진행할 수 있습니다."),
    ALREADY_PLAYING(HttpStatus.BAD_REQUEST,"125","게임 진행 중인 방에는 입장할 수 없습니다."),

    INTERNAL_SERVER_ERROR_PLZ_CHECK(HttpStatus.INTERNAL_SERVER_ERROR, "999", "알수없는 서버 내부 에러 발생 , dladlsgur3334@gmail.com 으로 연락 부탁드립니다.");

    private final HttpStatus httpStatus;
    private final String statusCode;
    private final String statusMsg;

    StatusCode(HttpStatus httpStatus, String statusCode, String statusMsg) {
        this.httpStatus = httpStatus;
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
    }
}