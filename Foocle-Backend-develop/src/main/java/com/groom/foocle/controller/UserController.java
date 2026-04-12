package com.groom.foocle.controller;


import com.groom.foocle.apiPayload.ApiResponse;
import com.groom.foocle.apiPayload.code.exception.GeneralException;
import com.groom.foocle.apiPayload.code.status.ErrorStatus;
import com.groom.foocle.common.security.JwtTokenProvider;
import com.groom.foocle.dto.req.UserDtoReq;
import com.groom.foocle.dto.res.UserDtoRes;
import com.groom.foocle.global.util.CookieUtil;
import com.groom.foocle.service.UserService;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

//    @Operation(summary = "앱 카카오로그인 API", description = "앱에서 카카오 로그인")
//    @PostMapping("/kakao-login")
//    public ApiResponse<UserDtoRes.UserLoginRes> kakaoLogin(@RequestBody @Valid AccessTokenRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
//        KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(request.getAccessToken());
//        User user = userService.kakaoSignup(userInfo);
//        return ApiResponse.onSuccess(userService.kakaoLogin(httpRequest, httpResponse, user));
//    }

//    @Operation(summary = "이메일 로그인 API(테스트용)", description = "이메일로 JWT토큰 발급")
//    @PostMapping("/login")
//    public ApiResponse<UserDtoRes.UserLoginRes> login(@RequestBody @Valid UserDtoReq.LoginReq loginDto, HttpServletRequest request, HttpServletResponse response) {
//        return ApiResponse.onSuccess(userService.login(request,response,loginDto));
//    }

//    @Operation(summary = "로그아웃 API", description = "액세스 토큰을 무효화하여 로그아웃")
//    @PostMapping("/logout")
//    public ApiResponse<SuccessStatus> logout(
//            @RequestHeader(value = "Authorization", required = false) String accessToken,
//            HttpServletRequest request, HttpServletResponse response) {
//
//        userService.logout(request, response, accessToken);
//        return ApiResponse.onSuccess(SuccessStatus._OK);
//    }

    @Operation(summary = "로컬 회원가입(웹)", description = "이메일/비밀번호로 회원가입하고 토큰 발급 + RT 쿠키 저장")
    @PostMapping("/local/signup")
    public ApiResponse<UserDtoRes.UserLoginRes> signUpLocalWeb(
            @RequestBody @Valid UserDtoReq.SignUpReq req,
            HttpServletRequest request, HttpServletResponse response) {

        userService.signUpLocal(req.getEmail(), req.getPassword(), req.getName());

        // 가입 직후 자동 로그인처럼 토큰 발급 & 쿠키
        var res = userService.loginLocalWeb(request, response, req.getEmail(), req.getPassword());

        // 웹: 바디에서 RT 제거
        res.setRefreshToken(null);

        return ApiResponse.onSuccess(res);
    }

    @Operation(summary = "로컬 로그인(웹)", description = "이메일/비밀번호로 로그인하고 토큰 발급 + RT 쿠키 저장")
    @PostMapping("/local/login")
    public ApiResponse<UserDtoRes.UserLoginRes> loginLocalWeb(
            @RequestBody @Valid UserDtoReq.LoginReq req,
            HttpServletRequest request, HttpServletResponse response) {

        // DTO에서 password가 @NotBlank가 아니므로 빈 값 방어
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
        var res = userService.loginLocalWeb(request, response, req.getEmail(), req.getPassword());

        // 웹: 바디에서 RT 제거 (쿠키만 사용)
        res.setRefreshToken(null);

        return ApiResponse.onSuccess(res);
    }

    @Operation(summary = "웹용-로그아웃 API", description = "액세스 토큰을 무효화하고 쿠키 삭제")
    @PostMapping("/logout")
    public ApiResponse<String> logoutWeb(
            @RequestHeader(value = "Authorization", required = false) String accessToken,
            HttpServletRequest request, HttpServletResponse response) {

        userService.logoutWeb(request, response, accessToken);
        return ApiResponse.onSuccess("로그아웃 성공이요~");
    }

    @Operation(summary = "웹용-토큰 재발급 API", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰 발급")
    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(HttpServletRequest request,  HttpServletResponse response) {
        // RT 추출(헤더 우선, 없으면 쿠키)
        String refreshToken = jwtTokenProvider.resolveRefreshToken();
        if (refreshToken == null) {
            throw new GeneralException(ErrorStatus.JWT_REFRESH_TOKEN_NOT_FOUND); // 기존 JWT_EMPTY보다 의미 정확
        }

        Long userId;
        try {
            userId = jwtTokenProvider.getUserIdInToken(refreshToken);
        } catch (JwtException e) {
            throw new GeneralException(ErrorStatus.JWT_REFRESH_TOKEN_INVALID);
        }

        // 저장된 RT와 일치 + 유효성 검사
        if (!jwtTokenProvider.validateRefreshToken(refreshToken, userId)) {
            throw new GeneralException(ErrorStatus.JWT_REFRESH_TOKEN_INVALID);
        }

        // 새 AT/RT 발급 (회전)
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId); // Redis의 RT:{userId} 값이 교체됨

        // 웹: RT를 쿠키로 재설정 (앱은 바디의 refreshToken을 써도 됨)
        CookieUtil.deleteCookie(request, response, "refreshToken");
        CookieUtil.addCookie(response, "refreshToken", newRefreshToken, JwtTokenProvider.REFRESH_TOKEN_VALID_TIME_IN_COOKIE);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
//        tokens.put("refreshToken", newRefreshToken); //앱 용

        return ApiResponse.onSuccess(tokens);
    }

//    @Operation(summary = "회원정보 설정/수정 API", description = "닉네임,출생년도,신분,입시유형을 수정합니다.")
//    @PatchMapping ("")
//    public ApiResponse<String> updateUserProfile(@RequestBody @Valid UserDtoReq.userProfileReq userProfileReq) {
//        Long userId = jwtTokenProvider.getUserIdFromToken();
//        userService.setUser(userId, userProfileReq);
//        return ApiResponse.onSuccess("회원정보 수정 완료");
//    }
//
//    @Operation(summary = "회원 기본 정보 조회 API", description = "사용자의 기본 정보를 조회합니다.")
//    @GetMapping("/basic-info")
//    public ApiResponse<UserDtoRes.userProfileRes> getUserBasicInfo() {
//        Long userId = jwtTokenProvider.getUserIdFromToken();
//        UserDtoRes.userProfileRes userBasicInfo = userService.getUserBasicInfo(userId);
//        return ApiResponse.onSuccess(userBasicInfo);
//    }

}
