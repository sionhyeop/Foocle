package com.groom.foocle.service;

import com.groom.foocle.dto.req.UserDtoReq;
import com.groom.foocle.dto.res.KakaoUserInfoResponseDto;
import com.groom.foocle.dto.res.UserDtoRes;
import com.groom.foocle.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

public interface UserService {
    UserDtoRes.UserLoginRes loginLocal(String email, String rawPassword);
    User signUpLocal(String email, String rawPassword, String name);
    UserDtoRes.UserLoginRes loginLocalWeb(HttpServletRequest request, HttpServletResponse response, String email, String rawPassword);
    UserDtoRes.UserLoginRes login(HttpServletRequest request, HttpServletResponse response, UserDtoReq.LoginReq loginDto);
    void logout(HttpServletRequest request, HttpServletResponse response, String accessToken);
    void logoutWeb(HttpServletRequest request, HttpServletResponse response, String accessToken);
    User kakaoSignup(KakaoUserInfoResponseDto userInfo);
//    UserDtoRes.UserLoginRes kakaoLogin(HttpServletRequest request, HttpServletResponse response, User user);
    UserDtoRes.UserLoginRes kakaoLoginWeb(HttpServletRequest request, HttpServletResponse response, User user);
//    void setUser(Long userId, UserDtoReq.userProfileReq userProfileReq);
//    UserDtoRes.userProfileRes getUserBasicInfo(Long userId);
}
