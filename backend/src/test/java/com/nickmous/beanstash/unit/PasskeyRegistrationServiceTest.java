package com.nickmous.beanstash.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nickmous.beanstash.domain.security.RoleService;
import com.nickmous.beanstash.domain.security.passkey.PasskeyRegistrationService;
import com.nickmous.beanstash.domain.security.passkey.PendingPasskeyRegistration;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;

@ExtendWith(MockitoExtension.class)
class PasskeyRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebAuthnRelyingPartyOperations rpOps;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private PasskeyRegistrationService service;

    private static PendingPasskeyRegistration profile() {
        return new PendingPasskeyRegistration("newuser", "new@test.com", "First", "Last");
    }

    @Test
    void requestRegistrationOptions_rejectsDuplicateUsername() {
        when(userRepository.findByUsername("existing")).thenReturn(new User());

        assertThatThrownBy(() -> service.requestRegistrationOptions("existing"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requestRegistrationOptions_callsRpOpsWithAuthentication() {
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(rpOps.createPublicKeyCredentialCreationOptions(any()))
            .thenReturn(PublicKeyCredentialCreationOptions.builder().build());

        service.requestRegistrationOptions("newuser");

        ArgumentCaptor<PublicKeyCredentialCreationOptionsRequest> captor =
            ArgumentCaptor.forClass(PublicKeyCredentialCreationOptionsRequest.class);
        verify(rpOps).createPublicKeyCredentialCreationOptions(captor.capture());
        assertThat(captor.getValue().getAuthentication().getName()).isEqualTo("newuser");
    }

    @Test
    void requestRegistrationOptions_returnsCreationOptions() {
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        PublicKeyCredentialCreationOptions expectedOptions =
            PublicKeyCredentialCreationOptions.builder().build();
        when(rpOps.createPublicKeyCredentialCreationOptions(any())).thenReturn(expectedOptions);

        PublicKeyCredentialCreationOptions result = service.requestRegistrationOptions("newuser");

        assertThat(result).isSameAs(expectedOptions);
    }

    @Test
    void requestRegistrationOptions_doesNotCreateAccount() {
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(rpOps.createPublicKeyCredentialCreationOptions(any()))
            .thenReturn(PublicKeyCredentialCreationOptions.builder().build());

        service.requestRegistrationOptions("newuser");

        // Options is preparatory: no user is persisted and no role assigned until the ceremony
        // completes, so an abandoned attempt does not reserve the username.
        verify(userRepository, never()).save(any());
        verify(roleService, never()).assignDefaultRole(any());
    }

    @Test
    void completeRegistration_registersCredential() {
        RelyingPartyRegistrationRequest request = Mockito.mock(RelyingPartyRegistrationRequest.class);

        service.completeRegistration(request, profile());

        verify(rpOps).registerCredential(request);
    }

    @Test
    void completeRegistration_createsUserWithNullPasswordAndDefaultRole() {
        RelyingPartyRegistrationRequest request = Mockito.mock(RelyingPartyRegistrationRequest.class);

        service.completeRegistration(request, profile());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("new@test.com");
        assertThat(savedUser.getFirstName()).isEqualTo("First");
        assertThat(savedUser.getLastName()).isEqualTo("Last");
        assertThat(savedUser.getPassword()).isNull();

        verify(roleService).assignDefaultRole(savedUser);
    }
}
