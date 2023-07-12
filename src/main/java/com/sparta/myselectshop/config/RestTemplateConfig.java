package com.sparta.myselectshop.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/*빈으로 수동 등록하는 방법*/

@Configuration 
// 설정과 관련된 빈으로 등록하는 애너테이션 //클래스 레벨에서 선언
// 직접 컨트롤이 가능한 내부 클래스에 사용

public class RestTemplateConfig  {
    @Bean
    //빈으로 등록하는 애너테이션 //메서드 레벨에서 선언
    // 주로 개발자가 컨트롤이 불가능한 외부 라이브러리를 받아오는 메서드를 빈으로 등록
    
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                // RestTemplate 으로 외부 API 호출 시 일정시간이 지나도 응답이 없을 때 무한대기 상태 방지를 위해 강제 종료 설정
                .setConnectTimeout(Duration.ofSeconds(5)) // 5초
                .setReadTimeout(Duration.ofSeconds(5)) // 5초
                .build();
    }
}