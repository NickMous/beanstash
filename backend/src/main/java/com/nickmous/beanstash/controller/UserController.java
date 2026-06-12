package com.nickmous.beanstash.controller;

import com.nickmous.beanstash.controller.dto.user.request.ReadRequest;
import com.nickmous.beanstash.controller.dto.user.response.ReadResponse;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.entity.UserType;
import com.nickmous.beanstash.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserController {
    private UserRepository userRepository;

    @GetMapping("/api/v1/users")
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

    @GetMapping("/api/v1/user/{username}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ReadResponse> read(ReadRequest request) {
        User user = userRepository.findByUsername(request.username());

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
}
