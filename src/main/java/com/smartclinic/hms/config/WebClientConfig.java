package com.smartclinic.hms.config;

import reactor.netty.http.client.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${llm.service.url:${LLM_SERVICE_URL:http://localhost:8000}}")
    private String llmServiceUrl;

    @Value("${llm.service.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${llm.service.timeout.read:30000}")
    private int readTimeout;

    @Bean
    public WebClient llmWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout));

        log.info("LLM WebClient 설정 - URL: {}, connectTimeout: {}ms, readTimeout: {}ms",
                llmServiceUrl, connectTimeout, readTimeout);

        return WebClient.builder()
                .baseUrl(llmServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
