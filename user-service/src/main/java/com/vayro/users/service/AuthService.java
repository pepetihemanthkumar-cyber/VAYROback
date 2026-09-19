package com.vayro.users.service;

import com.vayro.users.dto.AuthResponse;
import com.vayro.users.dto.LoginRequest;
import com.vayro.users.dto.RegisterRequest;
import com.vayro.users.dto.UserResponse;
import com.vayro.users.entity.Role;
import com.vayro.users.entity.User;
import com.vayro.users.exception.InvalidCredentialsException;
import com.vayro.users.exception.UserAlreadyExistsException;
import com.vayro.users.repository.UserRepository;
import com.vayro.users.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.vayro.users.dto.GoogleAuthRequest;
import org.springframework.beans.factory.annotation.Value;
import java.util.Collections;
import java.util.Optional;

/**
 * Service managing user registration and authentication workflows.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.google.client-id:${GOOGLE_CLIENT_ID:718836314167-kekvffi6ugr8s6gfg6ds00qb190ee12s.apps.googleusercontent.com}}")
    private String googleClientId;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone().trim());
        user.setRole(Role.USER); // Public registration strictly assigns USER role
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(token, UserResponse.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("Account is disabled. Please contact support.");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, UserResponse.fromEntity(user));
    }

    private volatile GoogleIdTokenVerifier cachedVerifier;

    private GoogleIdTokenVerifier getVerifier() {
        if (cachedVerifier == null) {
            synchronized (this) {
                if (cachedVerifier == null) {
                    GoogleIdTokenVerifier.Builder verifierBuilder = new GoogleIdTokenVerifier.Builder(
                            new NetHttpTransport(),
                            new GsonFactory()
                    );
                    if (googleClientId != null && !googleClientId.trim().isEmpty()) {
                        verifierBuilder.setAudience(Collections.singletonList(googleClientId.trim()));
                    }
                    cachedVerifier = verifierBuilder.build();
                }
            }
        }
        return cachedVerifier;
    }

    @Transactional
    public AuthResponse authenticateGoogleUser(GoogleAuthRequest request) {
        String tokenString = request.getCredential();
        if (tokenString == null || tokenString.trim().isEmpty()) {
            throw new InvalidCredentialsException("Google credential token is missing");
        }

        String googleSub = null;
        String email = null;
        String firstName = "User";
        String lastName = "Member";

        try {
            GoogleIdTokenVerifier verifier = getVerifier();
            GoogleIdToken idToken = verifier.verify(tokenString);

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                googleSub = payload.getSubject();
                email = payload.getEmail();
                if (payload.get("given_name") != null) {
                    firstName = (String) payload.get("given_name");
                }
                if (payload.get("family_name") != null) {
                    lastName = (String) payload.get("family_name");
                } else if (payload.get("name") != null) {
                    lastName = (String) payload.get("name");
                }
            } else {
                throw new InvalidCredentialsException("Invalid or expired Google credential token");
            }
        } catch (InvalidCredentialsException ice) {
            throw ice;
        } catch (Exception e) {
            throw new InvalidCredentialsException("Google authentication verification failed: " + e.getMessage());
        }

        if (email == null || email.trim().isEmpty()) {
            throw new InvalidCredentialsException("Google account email could not be verified");
        }

        String normalizedEmail = email.trim().toLowerCase();

        Optional<User> existingUserOpt = userRepository.findByGoogleSubjectId(googleSub);
        if (existingUserOpt.isEmpty()) {
            existingUserOpt = userRepository.findByEmail(normalizedEmail);
        }

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.getGoogleSubjectId() == null && googleSub != null) {
                user.setGoogleSubjectId(googleSub);
                userRepository.save(user);
            }
        } else {
            user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(normalizedEmail);
            user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            user.setPhone("+91 98765 43210");
            user.setGoogleSubjectId(googleSub);
            user.setRole(Role.USER); // Google authentication strictly creates regular USER role
            user.setEnabled(true);
            user = userRepository.save(user);
        }

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("Account is disabled. Please contact support.");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, UserResponse.fromEntity(user));
    }
}
