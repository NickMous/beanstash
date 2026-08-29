package com.nickmous.beanstash.annotations;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents the 404 the user endpoints return when no user matches the
 * {@code username} path variable.
 *
 * <p>springdoc derives responses from the method signature, so the
 * {@code ResponseEntity.notFound()} branch inside the method body is invisible
 * to it and has to be declared explicitly.</p>
 *
 * <p>The empty {@link Content} is deliberate: without it springdoc attaches the
 * operation's success schema to the 404 as well, which would not match the
 * empty body {@code notFound().build()} actually sends.</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "404", description = "No user with that username", content = @Content)
public @interface UserNotFoundResponse {
}
