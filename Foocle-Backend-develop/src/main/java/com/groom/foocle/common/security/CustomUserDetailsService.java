package com.groom.foocle.common.security;

import com.groom.foocle.apiPayload.code.exception.GeneralException;
import com.groom.foocle.apiPayload.code.status.ErrorStatus;
import com.groom.foocle.entity.User;
import com.groom.foocle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userPk)  {
        User user = userRepository.findById(Long.parseLong(userPk))
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        return new CustomUserDetail(user);
    }	// 위에서 생성한 CustomUserDetails Class
}

