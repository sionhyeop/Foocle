package com.groom.foocle.controller;


import com.groom.foocle.apiPayload.ApiResponse;
import com.groom.foocle.dto.res.KakaoUserInfoResponseDto;
import com.groom.foocle.dto.res.UserDtoRes;
import com.groom.foocle.service.KakaoService;
import com.groom.foocle.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("")
public class SocialLoginController {

    private final KakaoService kakaoService;
    private final UserService userService;

    @Deprecated
    @GetMapping("/kakao/callback")
    public ApiResponse<UserDtoRes.UserLoginRes> callback(@RequestParam("code") String code, HttpServletRequest request, HttpServletResponse response) {
        String accessToken = kakaoService.getAccessTokenFromKakao(code);
        KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(accessToken);

        //회원가입, 로그인 동시진행
        return ApiResponse.onSuccess(userService.kakaoLoginWeb(request,response, userService.kakaoSignup(userInfo)));
    }
}
