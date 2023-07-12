package com.sparta.myselectshop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.myselectshop.dto.KakaoUserInfoDto;
import com.sparta.myselectshop.entity.User;
import com.sparta.myselectshop.entity.UserRoleEnum;
import com.sparta.myselectshop.jwt.JwtUtil;
import com.sparta.myselectshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Slf4j(topic = "KAKAO Login")
@Service
@RequiredArgsConstructor
public class KakaoService {
    // Authorize 요청으로 정보 제공 동의
    // Redirect URI 에 code 파라미터 전달
    // 이때 코드는 Authorization Code(인증 코드,인가 코드)
    // 인증 코드를 통해 엑세스 토큰 발급

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    private final RestTemplate restTemplate;
    //Spring 내장 클래스로 HTTP 프로토콜의 메서드들에 적합한 여러 메서드를 제공함으로써, 간편하게 Rest 방식의 API 를 호출 즉, 요청 후 응답받을 수 있게 설계되어 있다.
    //json, xml response 를 모두 받을 수 있다.
    //Spring5.0 부터는 (동기식 API 인 RestTemplate 과 달리) WebClient 라는 새로운 비동기 접근 방식이 지원되어 RestTemplate 는 deprecated 되었다.
    //RestTemplate 가 아니라 RestTemplateConfig 에서 추가 옵션을 설정해준 상태

    private final JwtUtil jwtUtil;

    public String kakaoLogin(String code) throws JsonProcessingException {
    //kakaoLogin 의 매개변수인 String code : Controller 단에서 받아온 인증 코드

        // 1. "인가 코드"로 "액세스 토큰" 요청
        String accessToken = getToken(code);

        // 2. 토큰으로 카카오 API 호출 : "액세스 토큰"으로 "카카오 사용자 정보" 가져오기
        KakaoUserInfoDto kakaoUserInfo = getKakaoUserInfo(accessToken);

        // 3. 필요시에 회원가입
        User kakaoUser = registerKakaoUserIfNeeded(kakaoUserInfo);

        // 4. JWT 토큰 반환
        String createToken = jwtUtil.createToken(kakaoUser.getUsername(), kakaoUser.getRole());

        return createToken;
    }

    /* 1. 액세스 토큰 요청 로직 */
    private String getToken(String code) throws JsonProcessingException {
        //kakaoDevelopers 에서 요구하는 형식으로 작성하여, RestTemplate 으로 API 를 받아온다.

        //인증 코드 잘 받아오는지 log 로 확인
        log.info("인증코드 : " + code);

        // 요청 URL 만들기
        URI uri = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com")
                .path("/oauth/token")
                .encode()
                .build()
                .toUri();

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", "19bfbf5e63b8fdbe83f52769d81890c1");
        body.add("redirect_uri", "http://localhost:8080/api/user/kakao/callback");
        body.add("code", code);

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity
                .post(uri) //body 가 있으니까 post
                .headers(headers)
                .body(body);

        /* HTTP 요청 (인가 코드) 보내기 */
        //인가 코드로 액세스 토큰 발급 from 카카오 서버
        ResponseEntity<String> response = restTemplate.exchange(
                requestEntity,
                String.class //String 타입으로 HTTP 응답 받아오기
        );

        /* HTTP 응답 (JSON) -> 액세스 토큰 파싱 */
        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        //body 에 들어있던 액세스 토큰 값을 JsonNode 객체에 넣기
        //response.getBody() : 발급받아온 엑세스 토큰 가져오기

        return jsonNode.get("access_token").asText();
        //JsonNode 로부터 액세스 토큰을 get. 이는 asText() 에 의해 String 타입으로 반환이 된다.
    }
    

    /* 2. 사용자 정보 요청 */
    private KakaoUserInfoDto getKakaoUserInfo(String accessToken) throws JsonProcessingException {
    //카카오 사용자 정보를 요청하기 위해서는 액세스 토큰과 함께 요청을 보내야 한다.

        //액세스 토큰 잘 받아오는지 log 로 확인
        log.info("accessToken : " + accessToken);

        // 요청 URL 만들기
        URI uri = UriComponentsBuilder
                .fromUriString("https://kapi.kakao.com")
                .path("/v2/user/me")
                .encode()
                .build()
                .toUri();

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        //"Bearer "은 토큰이라는 것을 알려주는 식별자 역할을 한다.

        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(new LinkedMultiValueMap<>());

        // HTTP 요청 보내기 (RestTemplate 이용해서 String 타입으로 받아오기)
        ResponseEntity<String> response = restTemplate.exchange(
                requestEntity,
                String.class
        );

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        //JsonNode?

        Long id = jsonNode.get("id").asLong();
        String nickname = jsonNode.get("properties")
                .get("nickname").asText();
        String email = jsonNode.get("kakao_account")
                .get("email").asText();

        /* 넘어오는 Json 정보 형태 */
        // {
        //    "id":163233571,
        //    "properties": {
        //         "nickname": "르탄이",
        //         "profile_image" : "http://k.kakaocdm.net/...jpg", ...
        //     },
        //     "kakao_account": {
        //          "email": "letan@sparta.com", ...
        //     }
        // }
        // 여기에서 필요한 value 만 key 값을 통해 뽑아가는 거다.

        
        //카카오 사용자 정보 잘 받아오는지 log 로 확인
        log.info("카카오 사용자 정보: " + id + ", " + nickname + ", " + email);

        //KakaoUserInfoDto 로 카카오 사용자 정보 전달
        return new KakaoUserInfoDto(id, nickname, email);
    }

    /* 3. 필요시에 회원가입 */
    private User registerKakaoUserIfNeeded(KakaoUserInfoDto kakaoUserInfo) {
        // DB 에 중복된 Kakao Id 가 있는지 확인
        Long kakaoId = kakaoUserInfo.getId();
        User kakaoUser = userRepository.findByKakaoId(kakaoId).orElse(null);

        if (kakaoUser == null) {
            // 카카오 사용자 email 동일한 email 가진 회원이 있는지 확인
            String kakaoEmail = kakaoUserInfo.getEmail();
            User sameEmailUser = userRepository.findByEmail(kakaoEmail).orElse(null);
            if (sameEmailUser != null) {
                kakaoUser = sameEmailUser;
                // 기존 회원정보에 카카오 Id 추가
                kakaoUser = kakaoUser.kakaoIdUpdate(kakaoId);
            } else {
                // 신규 회원가입
                // password: random UUID
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);

                // email: kakao email
                String email = kakaoUserInfo.getEmail();

                kakaoUser = new User(kakaoUserInfo.getNickname(), encodedPassword, email, UserRoleEnum.USER, kakaoId);
            }

            userRepository.save(kakaoUser);
        }
        return kakaoUser;
    }
}