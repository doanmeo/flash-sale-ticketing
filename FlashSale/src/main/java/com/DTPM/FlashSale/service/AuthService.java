package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.request.ChangePasswordRequest;
import com.DTPM.FlashSale.dto.request.LoginRequest;
import com.DTPM.FlashSale.dto.request.LogoutRequest;
import com.DTPM.FlashSale.dto.request.RefreshTokenRequest;
import com.DTPM.FlashSale.dto.request.RegisterRequest;
import com.DTPM.FlashSale.dto.response.AuthResponse;
import com.DTPM.FlashSale.entity.RefreshToken;
import com.DTPM.FlashSale.entity.User;
import com.DTPM.FlashSale.enums.UserRole;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.mapper.UserMapper;
import com.DTPM.FlashSale.repository.RefreshTokenRepository;
import com.DTPM.FlashSale.repository.UserRepository;
import com.DTPM.FlashSale.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        UserRole role = request.getRole();
        if (role == UserRole.ADMIN) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Registering as ADMIN is not allowed");
        }
        if (role == null) {
            role = UserRole.USER;
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}, email: {}", savedUser.getId(), savedUser.getEmail());

        return createAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!user.canLogin()) {
            throw new AppException(ErrorCode.USER_BANNED);
        }

        log.info("User logged in successfully with id: {}", user.getId());
        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (!tokenEntity.isValid()) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = tokenEntity.getUser();
        if (!user.canLogin()) {
            throw new AppException(ErrorCode.USER_BANNED);
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(tokenEntity.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationInSeconds())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Transactional
    public void logout(LogoutRequest request) {
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElse(null);

        if (tokenEntity != null) {
            tokenEntity.revoke();
            refreshTokenRepository.save(tokenEntity);
            log.info("Revoked refresh token for user id: {}", tokenEntity.getUser().getId());
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Password changed successfully and all refresh tokens revoked for user id: {}", userId);
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenStr = UUID.randomUUID().toString();

        long refreshMs = jwtTokenProvider.getRefreshExpirationMs();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiresAt(LocalDateTime.now().plusNanos(refreshMs * 1_000_000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationInSeconds())
                .user(userMapper.toUserResponse(user))
                .build();
    }
}
