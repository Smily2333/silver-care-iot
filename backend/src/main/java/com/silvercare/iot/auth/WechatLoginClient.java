package com.silvercare.iot.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.silvercare.iot.config.WechatMiniappProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class WechatLoginClient {

    private final RestClient restClient;
    private final WechatMiniappProperties properties;

    public WechatLoginClient(RestClient.Builder builder, WechatMiniappProperties properties) {
        this.restClient = builder.baseUrl("https://api.weixin.qq.com").build();
        this.properties = properties;
    }

    public WechatSession exchange(String code) {
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getAppSecret())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WeChat login is not configured");
        }
        try {
            WechatSession response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/sns/jscode2session")
                            .queryParam("appid", properties.getAppId())
                            .queryParam("secret", properties.getAppSecret())
                            .queryParam("js_code", code)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .body(WechatSession.class);
            if (response == null || response.errcode() != null || !StringUtils.hasText(response.openid())) {
                String message = response == null ? "empty response" : response.errmsg();
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "WeChat login failed: " + message);
            }
            return response;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WeChat login service unavailable", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WechatSession(
            String openid,
            String unionid,
            @JsonProperty("session_key") String sessionKey,
            Integer errcode,
            String errmsg
    ) {
    }
}
