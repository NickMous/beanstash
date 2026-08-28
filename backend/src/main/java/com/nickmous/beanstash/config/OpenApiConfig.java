package com.nickmous.beanstash.config;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.webauthn.api.AttestationConveyancePreference;
import org.springframework.security.web.webauthn.api.AuthenticatorAttachment;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.COSEAlgorithmIdentifier;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;

/**
 * springdoc customization for Spring Security's WebAuthn value types.
 *
 * <p>These types each serialize on the wire as a bare scalar (their {@code WebauthnJacksonModule}
 * (de)serializers read/write a plain string or number), but springdoc introspects their
 * {@code getBytes()}/{@code getValue()} bean property and documents them as objects &mdash;
 * e.g. {@code {"bytes": "..."}} or {@code {"value": "public-key"}}. That mismatch is invisible to
 * Java but breaks the generated TypeScript client: every WebAuthn field (challenge, ids, rawId,
 * clientDataJSON, attestationObject, type, transports, attachment, alg, attestation, residentKey,
 * userVerification) gets wrapped/unwrapped incorrectly, so the typed client can neither read the
 * registration options nor post the credential &mdash; forcing raw {@code fetch}/{@code .raw.json()}
 * workarounds on the front end.
 *
 * <p>Mapping each type to its real scalar makes the OpenAPI schema &mdash; and therefore the
 * generated client and Swagger UI &mdash; match the actual JSON. The mappings are keyed on these
 * specific classes, so they only ever affect WebAuthn schemas.
 */
@Configuration
public class OpenApiConfig {

    static {
        SpringDocUtils config = SpringDocUtils.getConfig();

        // Serialized as a base64url string.
        config.replaceWithClass(Bytes.class, String.class);

        // Enum-like value types serialized as their string token.
        config.replaceWithClass(AuthenticatorAttachment.class, String.class);
        config.replaceWithClass(AuthenticatorTransport.class, String.class);
        config.replaceWithClass(PublicKeyCredentialType.class, String.class);
        config.replaceWithClass(AttestationConveyancePreference.class, String.class);
        config.replaceWithClass(ResidentKeyRequirement.class, String.class);
        config.replaceWithClass(UserVerificationRequirement.class, String.class);

        // COSE algorithm identifier is a (possibly negative) integer, e.g. -7 (ES256).
        config.replaceWithClass(COSEAlgorithmIdentifier.class, Long.class);
    }
}
