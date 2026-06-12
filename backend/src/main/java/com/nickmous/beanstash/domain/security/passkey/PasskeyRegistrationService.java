package com.nickmous.beanstash.domain.security.passkey;

import com.nickmous.beanstash.domain.security.RoleService;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PasskeyRegistrationService {

    private final UserRepository userRepository;
    private final WebAuthnRelyingPartyOperations rpOps;
    private final RoleService roleService;

    public PublicKeyCredentialCreationOptions requestRegistrationOptions(
            String username,
            String email,
            String firstName,
            String lastName
    ) {
        if (userRepository.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        Authentication auth = new UsernamePasswordAuthenticationToken(username, null, List.of());

        PublicKeyCredentialCreationOptionsRequest optionsRequest = () -> auth;

        PublicKeyCredentialCreationOptions creationOptions =
                rpOps.createPublicKeyCredentialCreationOptions(optionsRequest);

        userRepository.save(user);
        roleService.assignDefaultRole(user);

        return creationOptions;
    }

    public void completeRegistration(RelyingPartyRegistrationRequest request) {
        rpOps.registerCredential(request);
    }
}
