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

    /**
     * Builds the WebAuthn creation options for a new passkey. This is a preparatory step and
     * deliberately has no effect on the account: the {@link User} is created only once the
     * ceremony completes (see {@link #completeRegistration}). That way an abandoned or failed
     * registration leaves nothing persisted and the username stays available for another attempt.
     */
    public PublicKeyCredentialCreationOptions requestRegistrationOptions(String username) {
        if (userRepository.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(username, null, List.of());
        PublicKeyCredentialCreationOptionsRequest optionsRequest = () -> auth;

        return rpOps.createPublicKeyCredentialCreationOptions(optionsRequest);
    }

    /**
     * Verifies and stores the registered credential, then creates the passwordless {@link User}
     * from the {@code profile} captured when the options were issued. Creating the account here
     * &mdash; only after the credential is verified &mdash; is what keeps a failed ceremony from
     * permanently reserving the username.
     */
    public void completeRegistration(RelyingPartyRegistrationRequest request, PendingPasskeyRegistration profile) {
        rpOps.registerCredential(request);

        User user = new User();
        user.setUsername(profile.username());
        user.setEmail(profile.email());
        user.setFirstName(profile.firstName());
        user.setLastName(profile.lastName());

        userRepository.save(user);
        roleService.assignDefaultRole(user);
    }
}
