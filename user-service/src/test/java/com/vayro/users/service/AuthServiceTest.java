package com.vayro.users.service;

import com.vayro.users.dto.AuthResponse;
import com.vayro.users.dto.LoginRequest;
import com.vayro.users.dto.RegisterRequest;
import com.vayro.users.entity.Role;
import com.vayro.users.entity.User;
import com.vayro.users.exception.InvalidCredentialsException;
import com.vayro.users.exception.UserAlreadyExistsException;
import com.vayro.users.repository.UserRepository;
import com.vayro.users.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setFirstName("Rohan");
        sampleUser.setLastName("Verma");
        sampleUser.setEmail("rohan@example.com");
        sampleUser.setPassword("encodedPassword123");
        sampleUser.setPhone("9811122334");
        sampleUser.setRole(Role.USER);
        sampleUser.setEnabled(true);
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("Rohan", "Verma", "rohan@example.com", "Password123!", "9811122334");

        when(userRepository.existsByEmail("rohan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(sampleUser)).thenReturn("sample.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("sample.jwt.token", response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals("rohan@example.com", response.getUser().getEmail());
        assertEquals(Role.USER, response.getUser().getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsConflict() {
        RegisterRequest request = new RegisterRequest("Rohan", "Verma", "rohan@example.com", "Password123!", "9811122334");

        when(userRepository.existsByEmail("rohan@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("rohan@example.com", "Password123!");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("rohan@example.com", "Password123!"));
        when(userRepository.findByEmail("rohan@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(sampleUser)).thenReturn("sample.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("sample.jwt.token", response.getToken());
        assertEquals("rohan@example.com", response.getUser().getEmail());
    }

    @Test
    void testLoginBadCredentialsThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("rohan@example.com", "WrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}
