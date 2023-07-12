package com.sparta.myselectshop.mvc;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

/* 가짜 인증하기 -> Security가 동작하면 Controller를 테스트하는데 방해가 되기 때문에 가짜 Filter를 생성했다. */
public class MockSpringSecurityFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {}

    //SecurityContext : 인증객체를 담고있는 context 를 담는 공간
    //SecurityContextHolder : SecurityContext 에 접근하기 위해 사용
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        SecurityContextHolder.getContext() //getContext(): Security Context 반환
                .setAuthentication((Authentication) ((HttpServletRequest) req).getUserPrincipal()); //setAuthentication(): 인증 객체를 주는 메서드 //getUserPrincipal(): Authentication 객체로 바꿔 set
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        SecurityContextHolder.clearContext();
    }
}