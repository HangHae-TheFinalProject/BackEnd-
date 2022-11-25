package com.example.finalproject.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter(){
        UrlBasedCorsConfigurationSource source =new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);// 내서버가 응답을 할 떄 json을 자바스크립트에서 처리할 수 있게 할지를 설정하는 것
        // https 연결 안되면 얘부터 의심
//        config.addAllowedOrigin("*");//모든 ip의 응답을 허용하겠다. => 사용 X
//        config.addAllowedOrigin("http://localhost:8080");//모든 ip의 응답을 허용하겠다. => 사용 X
        config.addAllowedOrigin("https://haetae.shop/");//모든 ip의 응답을 허용하겠다. => 사용 X
        config.addAllowedOrigin("https://haetae.shop/5000");
        config.addAllowedOrigin("https://localhost:5000");
        config.addAllowedOrigin("https://localhost:8443");
        config.addAllowedOrigin("https://cheiks.shop/");
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("https://www.liergame.link/");
        config.addAllowedOriginPattern("*");//모든 ip의 응답을 허용하겠다. // 게시글 테스트 떄문에 잠시 열어둠
        config.addAllowedHeader("*");//모든 header의 응답을 허용하겠다.
        config.addAllowedMethod("*");//모든 post,get,putmdelete,patch 요청울 허용하겠다.
        config.addExposedHeader("*");
        source.registerCorsConfiguration("/lier/**",config);
        return new CorsFilter(source);
    }
}
