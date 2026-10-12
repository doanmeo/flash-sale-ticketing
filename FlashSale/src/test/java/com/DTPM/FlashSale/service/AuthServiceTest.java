package com.DTPM.FlashSale.service;

import com.DTPM.FlashSale.dto.request.LoginRequest;
import com.DTPM.FlashSale.dto.request.LogoutRequest;
import com.DTPM.FlashSale.dto.request.RefreshTokenRequest;
import com.DTPM.FlashSale.dto.request.RegisterRequest;
import com.DTPM.FlashSale.dto.response.AuthResponse;
import com.DTPM.FlashSale.dto.response.UserResponse;
import com.DTPM.FlashSale.domain.entity.RefreshToken;
import com.DTPM.FlashSale.domain.entity.User;
import com.DTPM.FlashSale.domain.enums.UserRole;
import com.DTPM.FlashSale.domain.enums.UserStatus;
import com.DTPM.FlashSale.exception.AppException;
import com.DTPM.FlashSale.exception.ErrorCode;
import com.DTPM.FlashSale.mapper.UserMapper;
import com.DTPM.FlashSale.domain.repository.RefreshTokenRepository;
import com.DTPM.FlashSale.domain.repository.UserRepository;
import com.DTPM.FlashSale.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private User bannedUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .fullName("Test User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        bannedUser = User.builder()
                .id(2L)
                .email("banned@example.com")
                .passwordHash("hashedPassword")
                .fullName("Banned User")
                .role(UserRole.USER)
                .status(UserStatus.BANNED)
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("register: Thất bại khi email đã tồn tại (USER_ALREADY_EXISTS)")
    void register_EmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .role(UserRole.USER)
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("register: Thất bại khi đăng ký quyền ADMIN")
    void register_AdminNotAllowed() {
        RegisterRequest request = RegisterRequest.builder()
                .email("admin@example.com")
                .password("password123")
                .fullName("Admin User")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    @DisplayName("register: Thành công tạo mới User và trả về AuthResponse")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@example.com")
                .password("password123")
                .fullName("New User")
                .role(UserRole.USER)
                .build();

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(User.builder().build());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(activeUser);
        when(userMapper.toUserResponse(activeUser)).thenReturn(userResponse);
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("mockJwtToken");
        when(jwtTokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(jwtTokenProvider.getJwtExpirationInSeconds()).thenReturn(900L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("test@example.com", response.getUser().getEmail());

        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login: Thất bại khi email không tồn tại (INVALID_CREDENTIALS)")
    void login_UserNotFound() {
        LoginRequest request = LoginRequest.builder()
                .email("notfound@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    @DisplayName("login: Thất bại khi mật khẩu sai (INVALID_CREDENTIALS)")
    void login_WrongPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    @DisplayName("login: Thất bại khi tài khoản bị cấm (USER_BANNED)")
    void login_UserBanned() {
        LoginRequest request = LoginRequest.builder()
                .email("banned@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("banned@example.com")).thenReturn(Optional.of(bannedUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.USER_BANNED, exception.getErrorCode());
    }

    @Test
    @DisplayName("login: Thành công đăng nhập và cấp token")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(userMapper.toUserResponse(activeUser)).thenReturn(userResponse);
        when(jwtTokenProvider.generateAccessToken(activeUser)).thenReturn("mockJwtToken");
        when(jwtTokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(jwtTokenProvider.getJwtExpirationInSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.getAccessToken());
        assertEquals("test@example.com", response.getUser().getEmail());
    }

    @Test
    @DisplayName("refreshToken: Thất bại khi refresh token không tồn tại hoặc đã revoked (INVALID_TOKEN)")
    void refreshToken_InvalidToken() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-token")
                .build();

        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("refreshToken: Thành công cấp accessToken mới")
    void refreshToken_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-token")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("valid-token")
                .user(activeUser)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(refreshToken));
        when(userMapper.toUserResponse(activeUser)).thenReturn(userResponse);
        when(jwtTokenProvider.generateAccessToken(activeUser)).thenReturn("newAccessToken");
        when(jwtTokenProvider.getJwtExpirationInSeconds()).thenReturn(900L);

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("valid-token", response.getRefreshToken());
    }

    @Test
    @DisplayName("logout: Thu hồi (revoke) refresh token thành công")
    void logout_Success() {
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("token-to-revoke")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("token-to-revoke")
                .user(activeUser)
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(refreshToken));

        authService.logout(request);

        assertTrue(refreshToken.getRevoked());
        verify(refreshTokenRepository, times(1)).save(refreshToken);
    }
}
