package com.epam.gym.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as not requiring authentication.
 * {@link AuthenticationInterceptor} skips the auth check entirely for
 * methods annotated with this (registration, login, public reference data, etc.).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoAuth {
}