package com.groom.foocle.service;

import com.groom.foocle.apiPayload.code.exception.GeneralException;
import com.groom.foocle.apiPayload.code.status.ErrorStatus;
import com.groom.foocle.common.security.JwtTokenProvider;
import com.groom.foocle.converter.UserConverter;
import com.groom.foocle.dto.req.UserDtoReq;
import com.groom.foocle.dto.res.KakaoUserInfoResponseDto;
import com.groom.foocle.dto.res.UserDtoRes;
import com.groom.foocle.entity.User;
import com.groom.foocle.entity.enums.Provider;
import com.groom.foocle.global.util.CookieUtil;
import com.groom.foocle.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

//    @Value("${cloud.aws.s3.path.profile}")
//    private String profilePath;

    // 테스트 용 로그인 처리: 이메일로 유저 찾고 토큰 발급
    @Transactional(readOnly = true)
    public UserDtoRes.UserLoginRes login(HttpServletRequest request, HttpServletResponse response, UserDtoReq.LoginReq loginDto) {
        String email = loginDto.getEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return UserConverter.signInRes(user, accessToken, refreshToken, user.getName());
    }


    // 로컬(이메일) 로그인 (쿠기x)
    @Transactional(readOnly = true)
    public UserDtoRes.UserLoginRes loginLocal(String email, String rawPassword) {

        // 카카오 계정이면 로컬 로그인 불가
        userRepository.findByEmailAndProvider(email, Provider.KAKAO).ifPresent(u -> {
            throw new GeneralException(
                    ErrorStatus.LOCAL_LOGIN_FOR_KAKAO_EMAIL
            );
        });

        User user = userRepository.findByEmailAndProvider(email, Provider.LOCAL)
                .orElseThrow(() -> new GeneralException(ErrorStatus._UNAUTHORIZED));

        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        return UserConverter.signInRes(user, accessToken, refreshToken, user.getName());
    }

    // 로컬(이메일) 로그인 (쿠키 포함)
    @Transactional(readOnly = true)
    public UserDtoRes.UserLoginRes loginLocalWeb(HttpServletRequest request, HttpServletResponse response,
                                                 String email, String rawPassword) {
        UserDtoRes.UserLoginRes res = loginLocal(email, rawPassword); // 기존 검증/AT·RT 발급 로직 재사용

        // 리프레시 토큰 쿠키 저장 (카카오 웹과 동일)
        CookieUtil.deleteCookie(request, response, "refreshToken");
        CookieUtil.addCookie(response, "refreshToken", res.getRefreshToken(),
                JwtTokenProvider.REFRESH_TOKEN_VALID_TIME_IN_COOKIE);

        return res;
    }


    // 로컬(이메일) 회원가입
    public User signUpLocal(String email, String rawPassword,  String name) {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }

        // 이미 '카카오'로 존재하는 이메일이면 가입 불가
        userRepository.findByEmailAndProvider(email, Provider.KAKAO).ifPresent(u -> {
            throw new GeneralException(
                    ErrorStatus.EMAIL_REGISTERED_WITH_KAKAO
            );
        });

        // 이미 LOCAL로 존재하면 중복
        if (userRepository.findByEmailAndProvider(email, Provider.LOCAL).isPresent()) {
            throw new GeneralException(
                    ErrorStatus.EMAIL_REGISTERED_WITH_LOCAL
            );
        }

        User user = User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(rawPassword))
                .provider(Provider.LOCAL)
                .build();

        return userRepository.save(user);
    }

    // 로그아웃 처리: 액세스 토큰 블랙리스트 추가 및 리프레시 토큰 삭제 (앱)
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, String accessToken) {
        String refreshToken = jwtTokenProvider.resolveRefreshToken();
        jwtTokenProvider.handleLogout(accessToken, refreshToken);
    }

    @Override
    public void logoutWeb(HttpServletRequest request, HttpServletResponse response, String accessToken) {
        String refreshToken = jwtTokenProvider.resolveRefreshToken();
        jwtTokenProvider.handleLogout(accessToken, refreshToken);
        // Cookie 에 있는 RefreshToken 의 데이터를 value 0, 만료 0 으로 초기화
        CookieUtil.addCookie(response, "refreshToken", null, 0);
    }


    // 카카오 로그인 시 신규 회원가입 또는 기존 회원 조회
    public User kakaoSignup(KakaoUserInfoResponseDto userInfo) {
        return userRepository.findByEmail(userInfo.getKakaoAccount().getEmail())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(userInfo.getKakaoAccount().getEmail())
                            .name(userInfo.getKakaoAccount().getProfile().getNickName())
                            .password(null)
                            .provider(Provider.KAKAO)
                            .build();
                    userRepository.save(newUser);
                    return newUser;
                });
    }

    // 카카오 로그인 처리 후 토큰 발급(앱)
//    public UserDtoRes.UserLoginRes kakaoLogin(HttpServletRequest request, HttpServletResponse response, User user) {
//        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
//        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
//
//        return UserConverter.signInRes(user, accessToken, refreshToken, user.getName());
//    }

    // 카카오 로그인 처리 후 토큰 발급(웹-쿠키 추가)
    public UserDtoRes.UserLoginRes kakaoLoginWeb(HttpServletRequest request, HttpServletResponse response, User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 리플레시 토큰 쿠키 저장
        CookieUtil.deleteCookie(request, response, "refreshToken");
        CookieUtil.addCookie(response, "refreshToken", refreshToken, JwtTokenProvider.REFRESH_TOKEN_VALID_TIME_IN_COOKIE);

        UserDtoRes.UserLoginRes res =
                UserConverter.signInRes(user, accessToken, refreshToken, user.getName());
        res.setRefreshToken(null); // 웹 응답에서 숨김
        return res;

    }

//    // 회원정보 저장
//    public void setUser(Long userId, UserDtoReq.userProfileReq userProfileReq) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
//
//        user.setBirthYear(userProfileReq.getBirthYear());
//        user.setRole(userProfileReq.getRole());
//
//        userRepository.save(user);
//    }
//
//    // 회원 기본 정보 조회
//    @Transactional(readOnly = true)
//    public UserDtoRes.userProfileRes getUserBasicInfo(Long userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
//
////        // 프로필 이미지 URL 생성
////        String profileImageUrl = null;
////        if (user.getProfileImage() != null) {
////            profileImageUrl = amazonS3Util.getProfileImageUrl(user.getProfileImage());
////        } else {
////            profileImageUrl = amazonS3Util.getDefaultProfileImageUrl();
////        }
////
//        return UserConverter.toUserProfileRes(user);
//    }

    //회원 정보 삭제(회원 탈퇴)
//    public void deleteUser(Long userId, HttpServletRequest request, HttpServletResponse response, String accessToken) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
//
//        // S3에서 프로필 이미지 삭제
//        if (user.getProfileImage() != null && user.getProfileImage().getUuid() != null) {
//            String profileKey = amazonS3Util.getProfileImageKey(user.getProfileImage());
//            amazonS3Util.deleteFile(profileKey);
//        }
//    }
//
//        //토큰 무효화
//        String refreshToken = jwtTokenProvider.resolveRefreshToken();
//        jwtTokenProvider.handleLogout(accessToken, refreshToken);
//
//        // 사용자 및 연관 데이터 삭제
//        userRepository.delete(user);
//    }

}

