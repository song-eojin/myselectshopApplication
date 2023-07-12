package com.sparta.myselectshop.config;


import com.sparta.myselectshop.jwt.JwtUtil;
import com.sparta.myselectshop.security.JwtAuthenticationFilter;
import com.sparta.myselectshop.security.JwtAuthorizationFilter;
import com.sparta.myselectshop.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

//----------------------------------------------------------

//인증(Authentication) ex. 로그인 : 특정 서비스에 일정 권한이 주어진 사용자임을 증명받는 행위
//인가(Authorization) ex. 글 작성, 좋아요, 댓글 달기 : 현재 권한이 주어진 상태이지만 서버에 요청해야 하는 활동들이 인가가 필요한 활동에 해당한다.

//----------------------------------------------------------

/*사용자의 로그인을 유지하는 방법*/

//1) 세션 : 활동기간이라는 의미로 예를 들어, 로그인과 로그아웃 사이의 활동 기간을 가리킨다.
//사용자의 로그인 요청에 대해 인증이 완료되었을 경우, 서버는 사용자들을 구분하기 위해 말교 기한이 있는 임시키(세션을 식별하기 위한 세션 id)를 발급하여 브라우저의 로컬 스토리지와 쿠키 등에 저장한다. 만약 세션 id를 브라우저 쿠키에 저장한다면, 세션 id 는 DB 에 저장되고 사용자가 서버에 요청을 보낼 때 마다 HTTP 요청 헤더에 담긴 세션 id 가 서버의 세션 id와 일치하는지 확인하는 과정을 매번 거치게 되는 것이다. 참고로 이러한 세션 인증/인가방식은 서버에서 세션을 관리하기 때문에 세션 id 를 탈취당하더라도 해당 세션을 만료시킬 수 있어 보안 상 유리하지만, 서버의 부담이 커지는 단점이 있다.

//2) 토큰 : 토큰 방식 中 jwt 를 이용하는 방식에 대해 살펴보면, jwt 를 이용하여 토큰 방식으로 인증/인가를 구현할 경우, 사용자가 정보를 입력하여 서버에 로그인 요청을 보내면, 서버는 해당 요청에서 받은 값을 이용하여 토큰을 발급한 하여 브라우저의 쿠키 등에 저장한다. 즉, 세션과 달리 서버 및 DB 에서는 토큰을 기억할 필요가 없는 것이다.

//CF. JWT
//jwt는 OOOOOO.OOOOOO.OOOOOO (영문 대소문자)와 같이 마침표를 기준으로 세 부분으로 나뉜다. 세 부분은 각각 header, payload, verify signature 라고 하며, header 와 payload 는 base 64 로, verigy signature 는 header 에 명시된 알고리즘에 의해 인코딩된다.
//header:



@Configuration
@EnableWebSecurity // Spring Security 지원을 가능하게 함
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationConfiguration authenticationConfiguration;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
        filter.setAuthenticationManager(authenticationManager(authenticationConfiguration));
        return filter;
    }

    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter() {
        return new JwtAuthorizationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF 설정
        http.csrf((csrf) -> csrf.disable());

        // 기본 설정인 Session 방식은 사용하지 않고 JWT 방식을 사용하기 위한 설정
        http.sessionManagement((sessionManagement) ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests((authorizeHttpRequests) ->
                authorizeHttpRequests
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll() // resources 접근 허용 설정
                        .requestMatchers("/").permitAll() // 메인 페이지 요청 허가
                        .requestMatchers("/api/user/**").permitAll() // '/api/user/'로 시작하는 요청 모두 접근 허가
                        .anyRequest().authenticated() // 그 외 모든 요청 인증처리
        );

        http.formLogin((formLogin) ->
                formLogin
                        .loginPage("/api/user/login-page").permitAll()
        );

        // 필터 관리
        http.addFilterBefore(jwtAuthorizationFilter(), JwtAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}