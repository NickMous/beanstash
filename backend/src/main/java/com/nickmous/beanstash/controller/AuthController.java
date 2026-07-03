package com.nickmous.beanstash.controller;

import com.nickmous.beanstash.controller.dto.auth.LoginRequest;
import com.nickmous.beanstash.controller.dto.auth.PasskeyRegistrationOptionsRequest;
import com.nickmous.beanstash.controller.dto.auth.RegisterRequest;
import com.nickmous.beanstash.controller.dto.auth.VerifyTotpRequest;
import com.nickmous.beanstash.domain.security.CustomUserDetailsService;
import com.nickmous.beanstash.domain.security.RoleService;
import com.nickmous.beanstash.domain.security.passkey.PasskeyRegistrationService;
import com.nickmous.beanstash.domain.security.passkey.PendingPasskeyRegistration;
import com.nickmous.beanstash.domain.security.totp.TotpService;
import com.nickmous.beanstash.domain.security.totp.TotpSetupResponse;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.management.ImmutableRelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey;
import org.springframework.security.web.webauthn.registration.HttpSessionPublicKeyCredentialCreationOptionsRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    // Session attribute holding the pending signup profile between the passkey options and
    // completion requests. See PendingPasskeyRegistration.
    private static final String PENDING_PASSKEY_REGISTRATION_ATTR =
        "com.nickmous.beanstash.passkey.PENDING_REGISTRATION";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final PasskeyRegistrationService passkeyRegistrationService;
    private final RoleService roleService;
    private final CustomUserDetailsService customUserDetailsService;

    @PostMapping(path = "/register", version = "v1")
    public ResponseEntity<TotpSetupResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        TotpSetupResponse totpSetup = totpService.setupTotp(user);
        roleService.assignDefaultRole(user);
        return ResponseEntity.ok(totpSetup);
    }

    @PostMapping(path = "/register/verify-totp", version = "v1")
    public ResponseEntity<Void> verifyTotp(@Valid @RequestBody VerifyTotpRequest request) {
        User user = userRepository.findByUsername(request.username());
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        if (totpService.verifyAndEnableTotp(user, request.code())) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    @PostMapping(path = "/login", version = "v1")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(request.username());
        if (user == null || user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (user.isTotpEnabled()) {
            if (request.totpCode() == null || !totpService.verifyCode(user, request.totpCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getUsername());

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        httpRequest.getSession(true);
        httpRequest.changeSessionId();
        httpRequest.getSession()
            .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/register/passkey/options", version = "v1")
    public ResponseEntity<PublicKeyCredentialCreationOptions> passkeyRegistrationOptions(
            @Valid @RequestBody PasskeyRegistrationOptionsRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            PublicKeyCredentialCreationOptions options =
                passkeyRegistrationService.requestRegistrationOptions(request.username());

            var optionsRepository = new HttpSessionPublicKeyCredentialCreationOptionsRepository();
            optionsRepository.save(httpRequest, httpResponse, options);

            // Hold the profile in the session until the ceremony completes; the account is created
            // only on success (POST /register/passkey), so an abandoned attempt leaves no orphaned
            // user and the username can be retried.
            httpRequest.getSession().setAttribute(
                PENDING_PASSKEY_REGISTRATION_ATTR,
                new PendingPasskeyRegistration(
                    request.username(), request.email(), request.firstName(), request.lastName()));

            return ResponseEntity.ok(options);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping(path = "/register/passkey", version = "v1")
    public ResponseEntity<Void> completePasskeyRegistration(
            @RequestBody RelyingPartyPublicKey publicKey,
            HttpServletRequest httpRequest) {
        var optionsRepository = new HttpSessionPublicKeyCredentialCreationOptionsRepository();
        PublicKeyCredentialCreationOptions options = optionsRepository.load(httpRequest);

        HttpSession session = httpRequest.getSession(false);
        PendingPasskeyRegistration pending = session == null ? null
            : (PendingPasskeyRegistration) session.getAttribute(PENDING_PASSKEY_REGISTRATION_ATTR);

        if (options == null || pending == null) {
            return ResponseEntity.badRequest().build();
        }

        var registrationRequest = new ImmutableRelyingPartyRegistrationRequest(options, publicKey);
        passkeyRegistrationService.completeRegistration(registrationRequest, pending);
        session.removeAttribute(PENDING_PASSKEY_REGISTRATION_ATTR);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(pending.username());

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        httpRequest.getSession(true);
        httpRequest.changeSessionId();
        httpRequest.getSession()
            .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return ResponseEntity.ok().build();
    }
}
