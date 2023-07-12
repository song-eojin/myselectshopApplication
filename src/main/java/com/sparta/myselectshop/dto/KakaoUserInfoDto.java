package com.sparta.myselectshop.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// KakaoService 로부터 KakaoUserInfoDto 로 카카오 사용자 정보 전달받아 카카오 유저 객체 하나가 생성한다 by new KakaoUserInfoDto(id, nickname, email)
@Getter
@NoArgsConstructor
public class KakaoUserInfoDto {
    private Long id;
    private String nickname;
    private String email;

    public KakaoUserInfoDto(Long id, String nickname, String email) {
        this.id = id;
        this.nickname = nickname;
        this.email = email;
    }
}