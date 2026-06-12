package com.nickmous.beanstash.controller.dto.user.response;

public record ReadResponse (
    String username,
    String email,
    String firstName,
    String lastName
) {
}
