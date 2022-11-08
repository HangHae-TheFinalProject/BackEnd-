package com.example.finalproject.service;

import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

// 메인 페이지 인터페이스화
public interface GameRoomService {
    // 메인 페이지
    ModelAndView displayMainPage(Long id, String uuid);

    //
    ModelAndView processRoomSelection(String sid, String uuid, BindingResult bindingResult);
    ModelAndView displaySelectedRoom(String sid, String uuid);
    ModelAndView processRoomExit(String sid, String uuid);
    ModelAndView requestRandomRoomNumber(String uuid);
}
