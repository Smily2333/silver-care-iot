package com.silvercare.iot.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silvercare.iot.config.WechatMiniappProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WechatLoginClientTest {

    @Test
    void parsesJsonReturnedAsPlainText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WechatMiniappProperties properties = new WechatMiniappProperties();
        properties.setAppId("test-app-id");
        properties.setAppSecret("test-app-secret");
        WechatLoginClient client = new WechatLoginClient(builder, properties, new ObjectMapper());

        server.expect(requestTo("https://api.weixin.qq.com/sns/jscode2session"
                        + "?appid=test-app-id&secret=test-app-secret&js_code=wx-code"
                        + "&grant_type=authorization_code"))
                .andExpect(queryParam("appid", "test-app-id"))
                .andExpect(queryParam("secret", "test-app-secret"))
                .andExpect(queryParam("js_code", "wx-code"))
                .andRespond(withSuccess("""
                        {"openid":"openid-1","session_key":"session-key-1"}
                        """, MediaType.TEXT_PLAIN));

        WechatLoginClient.WechatSession session = client.exchange("wx-code");

        assertThat(session.openid()).isEqualTo("openid-1");
        assertThat(session.sessionKey()).isEqualTo("session-key-1");
        server.verify();
    }
}
