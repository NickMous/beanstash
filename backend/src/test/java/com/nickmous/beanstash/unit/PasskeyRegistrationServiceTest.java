package com.nickmous.beanstash.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nickmous.beanstash.domain.security.RoleService;
import com.nickmous.beanstash.domain.security.passkey.PasskeyRegistrationService;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @Test
    void requestRegistrationOptions_rejectsDuplicateUsername() {
        when(userRepository.findByUsername("existing")).thenReturn(new User());

        assertThatThrownBy(() -> service.requestRegistrationOptions("existing", "e@test.com", "First", "Last"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requestRegistrationOptions_createsUserWithNullPassword() {
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(rpOps.createPublicKeyCredentialCreationOptions(any()))
            .thenReturn(PublicKeyCredentialCreationOptions.builder().build());

        service.requestRegistrationOptions("newuser", "new@test.com", "First", "Last");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("new@test.com");
        assertThat(savedUser.getPassword()).isNull();
    }

    @Test
    void requestRegistrationOptions_callsRpOpsWithAuthentication() {
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(rpOps.createPublicKeyCredentialCreationOptions(any()))
            .thenReturn(PublicKeyCredentialCreationOptions.builder().build());

        service.requestRegistrationOptions("newuser", "new@test.com", "First", "Last");

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

        PublicKeyCredentialCreationOptions result =
            service.requestRegistrationOptions("newuser", "new@test.com", "First", "Last");

        assertThat(result).isSameAs(expectedOptions);
    }

    @Test
    void requestRegistrationOptions_assignsDefaultRole() {
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(rpOps.createPublicKeyCredentialCreationOptions(any()))
            .thenReturn(PublicKeyCredentialCreationOptions.builder().build());

        service.requestRegistrationOptions("newuser", "new@test.com", "First", "Last");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(roleService).assignDefaultRole(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("newuser");
    }

    @Test
    void completeRegistration_callsRpOpsRegisterCredential() {
        RelyingPartyRegistrationRequest request = org.mockito.Mockito.mock(RelyingPartyRegistrationRequest.class);

        service.completeRegistration(request);

        verify(rpOps).registerCredential(request);
    }
}
