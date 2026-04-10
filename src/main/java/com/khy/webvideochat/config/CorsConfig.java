package com.khy.webvideochat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * @fileName    CorsConfig.java
 * @author      Hanyeol Ko
 * @date        2026-04-11
 * @description CORS 필터 설정.
 *              개발(dev): localhost:5173 허용.
 *              운영(prod): 환경변수 ALLOWED_ORIGINS 로 허용 오리진 목록 제어.
 *
 * History:
 * | Date       | Author     | Description |
 * |------------|------------|-------------|
 * | 2026-04-11 | Hanyeol Ko | 최초 작성     |
 */
@Configuration
public class CorsConfig {

  @Value("${cors.allowed-origins:http://localhost:5173}")
  private List<String> allowedOrigins;

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }
}
