package com.nickmous.beanstash.controller;

import com.nickmous.beanstash.controller.dto.user.response.ReadResponse;
import com.nickmous.beanstash.entity.User;
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

    @GetMapping("/api/v1/user/{username}")
    @PreAuthorize("hasAuthority('user:read')")
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
}
