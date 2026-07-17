package com.silvercare.iot.auth;

import com.silvercare.iot.config.WechatMiniappProperties;
import com.silvercare.iot.domain.entity.MiniappSession;
import com.silvercare.iot.domain.entity.MiniappUser;
import com.silvercare.iot.repository.MiniappSessionRepository;
import com.silvercare.iot.repository.MiniappUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniappAuthServiceTest {

    private final WechatLoginClient wechatLoginClient = mock(WechatLoginClient.class);
    private final MiniappUserRepository userRepository = mock(MiniappUserRepository.class);
    private final MiniappSessionRepository sessionRepository = mock(MiniappSessionRepository.class);

    @Test
    void loginCreatesHashedSessionAndTokenAuthenticates() {
        WechatMiniappProperties properties = new WechatMiniappProperties();
        properties.setSessionTtlDays(30);
        MiniappAuthService service = new MiniappAuthService(
                wechatLoginClient, userRepository, sessionRepository, properties);
        MiniappUser user = user(8L);
        when(wechatLoginClient.exchange("wx-code"))
                .thenReturn(new WechatLoginClient.WechatSession("openid-1", null, "session-key", null, null));
        when(userRepository.findByOpenid("openid-1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        MiniappAuthService.LoginResult result = service.login("wx-code");

        ArgumentCaptor<MiniappSession> sessionCaptor = ArgumentCaptor.forClass(MiniappSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        MiniappSession savedSession = sessionCaptor.getValue();
        assertThat(result.accessToken()).isNotBlank();
        assertThat(savedSession.getTokenHash()).hasSize(64).isNotEqualTo(result.accessToken());
        assertThat(savedSession.getUserId()).isEqualTo(8L);
        assertThat(savedSession.getExpiresAt()).isAfter(Instant.now().plusSeconds(29L * 24 * 3600));

        when(sessionRepository.findByTokenHashAndExpiresAtAfter(eq(savedSession.getTokenHash()), any(Instant.class)))
                .thenReturn(Optional.of(savedSession));
        assertThat(service.authenticate(result.accessToken()))
                .get().extracting(principal -> principal.userId()).isEqualTo(8L);
    }

    private MiniappUser user(Long id) {
        MiniappUser user = new MiniappUser();
        user.setOpenid("openid-1");
        try {
            var field = MiniappUser.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
            return user;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
