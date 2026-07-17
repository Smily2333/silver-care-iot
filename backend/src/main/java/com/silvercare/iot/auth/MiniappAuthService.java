package com.silvercare.iot.auth;

import com.silvercare.iot.config.WechatMiniappProperties;
import com.silvercare.iot.domain.entity.MiniappSession;
import com.silvercare.iot.domain.entity.MiniappUser;
import com.silvercare.iot.repository.MiniappSessionRepository;
import com.silvercare.iot.repository.MiniappUserRepository;
import com.silvercare.iot.security.MiniappPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class MiniappAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final WechatLoginClient wechatLoginClient;
    private final MiniappUserRepository userRepository;
    private final MiniappSessionRepository sessionRepository;
    private final WechatMiniappProperties properties;

    public MiniappAuthService(WechatLoginClient wechatLoginClient,
                              MiniappUserRepository userRepository,
                              MiniappSessionRepository sessionRepository,
                              WechatMiniappProperties properties) {
        this.wechatLoginClient = wechatLoginClient;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.properties = properties;
    }

    @Transactional
    public LoginResult login(String code) {
        WechatLoginClient.WechatSession wechatSession = wechatLoginClient.exchange(code);
        MiniappUser user = userRepository.findByOpenid(wechatSession.openid()).orElseGet(() -> {
            MiniappUser created = new MiniappUser();
            created.setOpenid(wechatSession.openid());
            return created;
        });
        if (wechatSession.unionid() != null) {
            user.setUnionid(wechatSession.unionid());
        }
        user = userRepository.save(user);

        String accessToken = newAccessToken();
        int sessionTtlDays = Math.max(1, properties.getSessionTtlDays());
        Instant expiresAt = Instant.now().plus(sessionTtlDays, ChronoUnit.DAYS);
        MiniappSession session = new MiniappSession();
        session.setUserId(user.getId());
        session.setTokenHash(hash(accessToken));
        session.setExpiresAt(expiresAt);
        sessionRepository.save(session);
        return new LoginResult(accessToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<MiniappPrincipal> authenticate(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }
        return sessionRepository.findByTokenHashAndExpiresAtAfter(hash(accessToken), Instant.now())
                .map(session -> new MiniappPrincipal(session.getUserId()));
    }

    private String newAccessToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public record LoginResult(String accessToken, Instant expiresAt) {
    }
}
