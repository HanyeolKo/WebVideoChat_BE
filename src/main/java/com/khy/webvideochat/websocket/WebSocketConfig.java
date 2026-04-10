package com.khy.webvideochat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.Map;

/**
 * @fileName    WebSocketConfig.java
 * @author      Hanyeol Ko
 * @date        2026-04-11
 * @description WebSocket 핸들러 등록 및 핸드쉐이크 인터셉터 설정.
 *
 * History:
 * | Date       | Author     | Description                              |
 * |------------|------------|------------------------------------------|
 * | 2026-04-11 | Hanyeol Ko | configuration 패키지에서 websocket으로 이동 |
 */
@Configuration
@EnableWebSocket
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

  @Autowired
  SocketHandler socketHandler;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(socketHandler, "/socket/{roomId}")
        .setAllowedOriginPatterns("*")
        .addInterceptors(handshakeInterceptor());
  }

  /**
   * 핸드쉐이크 시 URL에서 roomId를 추출해 ThreadLocal에 저장
   */
  private HandshakeInterceptor handshakeInterceptor() {
    return new HandshakeInterceptor() {
      @Override
      public boolean beforeHandshake(ServerHttpRequest request,
                                     ServerHttpResponse response,
                                     WebSocketHandler wsHandler,
                                     Map<String, Object> attributes) throws Exception {
        String path = request.getURI().getPath();
        String roomId = path.substring(path.lastIndexOf("/socket/") + 8);
        socketHandler.getRoomIdThreadLocal().set(roomId);
        log.info("WebSocket 핸드쉐이크 roomId: {}", roomId);
        return true;
      }

      @Override
      public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Exception exception) {
      }
    };
  }

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    container.setMaxTextMessageBufferSize(1024 * 1024);
    return container;
  }
}
