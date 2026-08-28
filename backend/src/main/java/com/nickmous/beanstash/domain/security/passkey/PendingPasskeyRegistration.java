package com.nickmous.beanstash.domain.security.passkey;

import jakarta.validation.constraints.Email;
import java.io.Serializable;

/**
 * The signup profile captured when passkey registration options are issued, held in the HTTP
 * session until the ceremony completes. The account is only created once the credential is
 * verified (see {@link PasskeyRegistrationService#completeRegistration}), so an abandoned or
 * failed registration leaves no orphaned user behind and the username stays available for retry.
 */
public record PendingPasskeyRegistration(
    String username,
    @Email String email,
    String firstName,
    String lastName
) implements Serializable {
}
