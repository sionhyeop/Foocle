package com.groom.foocle.apiPayload.code.status;


import com.groom.foocle.apiPayload.code.BaseCode;
import com.groom.foocle.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {
    _OK(HttpStatus.OK, "COMMON200", "성공!"),

    // 성공 관련 응답
    NO_DUPLICATE_NICKNAME(HttpStatus.OK, "COMMON2002","닉네임 생성이 가능"),
    SUCCESS_POST_RECIPE(HttpStatus.OK, "COMMON2002", "레시피 저장 완료");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build();
    }
}