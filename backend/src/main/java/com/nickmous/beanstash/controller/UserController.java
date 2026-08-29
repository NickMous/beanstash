package com.nickmous.beanstash.controller;

import com.nickmous.beanstash.annotations.UserNotFoundResponse;
import com.nickmous.beanstash.controller.dto.user.request.UpdateRequest;
import com.nickmous.beanstash.controller.dto.user.response.ReadResponse;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.entity.UserType;
import com.nickmous.beanstash.repository.UserRepository;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserController {
    private UserRepository userRepository;

    @GetMapping(path = "/users", version = "v1")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<Iterable<ReadResponse>> list() {
        Iterable<User> users = userRepository.findByType(UserType.HUMAN);

        Iterable<ReadResponse> response = () -> new java.util.Iterator<>() {
            private final java.util.Iterator<User> userIterator = users.iterator();

            @Override
            public boolean hasNext() {
                return userIterator.hasNext();
            }

            @Override
            public ReadResponse next() {
                User user = userIterator.next();
                return new ReadResponse(
                    user.getUsername(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName()
                );
            }
        };

        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/user/{username}", version = "v1")
    @PreAuthorize("hasAuthority('user:read')")
    @ApiResponse(responseCode = "200", description = "The requested user",
        content = @Content(schema = @Schema(implementation = ReadResponse.class)))
    @UserNotFoundResponse
    public ResponseEntity<ReadResponse> read(@PathVariable String username) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        ReadResponse response = new ReadResponse(
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName()
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping(path = "/user/{username}", version = "v1")
    @PreAuthorize("hasAuthority('user:write') or #username == authentication.name")
    @ApiResponse(responseCode = "200", description = "The updated user",
        content = @Content(schema = @Schema(implementation = ReadResponse.class)))
    @UserNotFoundResponse
    public ResponseEntity<ReadResponse> update(@PathVariable String username, @Valid @RequestBody UpdateRequest request) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.email() != null) {
            user.setEmail(request.email());
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }

        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }

        userRepository.save(user);

        ReadResponse response = new ReadResponse(
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName()
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping(path = "/user/{username}", version = "v1")
    @PreAuthorize("hasAuthority('user:write') or #username == authentication.name")
    @ApiResponse(responseCode = "204", description = "User deleted", content = @Content)
    @UserNotFoundResponse
    public ResponseEntity<Void> delete(@PathVariable String username) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        userRepository.delete(user);

        return ResponseEntity.noContent().build();
    }
}
