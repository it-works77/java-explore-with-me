package ru.practicum.ewm.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.client.StatsClientImpl;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class StatsClientConfig {
    @Value("${stats-service.url:http://localhost:9090}")
    private String statsServerUrl;

    @Value("${stats-service.connect-timeout:3s}")
    private Duration connectTimeout;

    @Value("${stats-service.read-timeout:5s}")
    private Duration readTimeout;

    @Bean
    public HttpClient statsHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    @Bean
    public RestTemplate statsClientRestTemplate(HttpClient statsHttpClient) {

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(statsHttpClient);
        requestFactory.setReadTimeout(readTimeout);
        return new RestTemplate(requestFactory);
    }

    @Bean
    public StatsClient statsClient(RestTemplate statsClientRestTemplate) {
        return new StatsClientImpl(statsClientRestTemplate, statsServerUrl);
    }
}
