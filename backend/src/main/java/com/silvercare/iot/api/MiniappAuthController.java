package com.silvercare.iot.api;

import com.silvercare.iot.auth.MiniappAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/miniapp/auth")
public class MiniappAuthController {

    private final MiniappAuthService authService;

    public MiniappAuthController(MiniappAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public MiniappAuthService.LoginResult login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.code());
    }

    public record LoginRequest(@NotBlank @Size(max = 128) String code) {
    }
}
