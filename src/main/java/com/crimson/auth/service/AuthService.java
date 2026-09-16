package com.crimson.auth.service;

import com.crimson.auth.dto.request.LoginRequest;
import com.crimson.auth.dto.request.RegisterRequest;
import com.crimson.auth.dto.response.AuthResponse;
import com.crimson.auth.dto.response.UserResponse;
import com.crimson.auth.entity.RefreshToken;
import com.crimson.auth.entity.Role;
import com.crimson.auth.entity.RoleName;
import com.crimson.auth.entity.User;
import com.crimson.auth.exception.AccountLockedException;
import com.crimson.auth.exception.TokenRefreshException;
import com.crimson.auth.exception.UserAlreadyExistsException;
import com.crimson.auth.repository.RoleRepository;
import com.crimson.auth.repository.UserRepository;
import com.crimson.auth.security.JwtService;
import com.crimson.auth.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-duration-minutes}")
    private long lockDurationMinutes;

    @Transactional
    public UserResponse register(RegisterRequest request){
        if(userRepository.existsByUsername(request.getUsername())){
            throw new UserAlreadyExistsException("username already exists");
        }

        if(userRepository.existsByEmail(request.getEmail())){
            throw new UserAlreadyExistsException("email already registered");
        }

        Role userRole = roleRepository.findByName((RoleName.ROLE_USER))
                .orElseThrow(() -> new IllegalStateException("ROLE_USER is not seeded in the database"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .build();

        User saved  = userRepository.save(user);
        log.info("new user registered: {}", saved.getUsername());

        return toUserResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request){
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new BadCredentialsException("invalid username/email or password"));

        checkAccountLockStatus(user);

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            registerFailedAttempt(user);
            throw new BadCredentialsException("invalid username/email or password");
        }

        if(!user.isEnabled()){
            throw new org.springframework.security.authentication.DisabledException("Account is disabled");
        }

        resetFailedAttempts(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name()).collect(Collectors.toList());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .user(toUserResponse(user))
                .roles(roleNames)
                .build();
    }


    @Transactional
    public AuthResponse refreshAccessToken(String requestRefreshToken) {
        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found"));

        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        RefreshToken rotated = refreshTokenService.createRefreshToken(user);

        List<String> roleNames = user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(rotated.getToken())
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .user(toUserResponse(user))
                .roles(roleNames)
                .build();
    }

    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + username));
        refreshTokenService.revokeByUser(user);
    }

    private void checkAccountLockStatus(User user) {
        if (!user.isAccountNonLocked()) {
            if (user.getLockTime() != null &&
                    Instant.now().isAfter(user.getLockTime().plus(lockDurationMinutes, ChronoUnit.MINUTES))) {
                // Lock window has passed — automatically unlock.
                user.setAccountNonLocked(true);
                user.setFailedAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            } else {
                throw new AccountLockedException(
                        "Account is locked due to too many failed login attempts. Try again later.");
            }
        }
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= maxFailedAttempts) {
            user.setAccountNonLocked(false);
            user.setLockTime(Instant.now());
            log.warn("Account locked after {} failed attempts: {}", attempts, user.getUsername());
        }
        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedAttempts() > 0) {
            user.setFailedAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .build();
    }


}
