package com.groom.foocle.converter;


import com.groom.foocle.dto.res.UserDtoRes;
import com.groom.foocle.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserConverter {
    public static UserDtoRes.UserLoginRes signInRes(User user, String accessToken, String refreshToken, String name) {
        return UserDtoRes.UserLoginRes.builder()
                .id(user.getId())
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .name(name)
                .build();
    }
}
