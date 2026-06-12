package com.nickmous.beanstash.entity;

/**
 * Distinguishes the kinds of principal stored in the user table.
 *
 * <ul>
 *   <li>{@code HUMAN} — a real, self-registered user; the only type returned by
 *       user-facing endpoints.</li>
 *   <li>{@code SYSTEM} — the singleton sentinel used as the audit actor when there
 *       is no human actor. Never authenticates and is hidden from all JPA queries
 *       (see {@code User}'s {@code @SQLRestriction}).</li>
 *   <li>{@code BOT} — a non-human actor that authenticates with its own credentials
 *       (tokens/API keys). Loadable like a human, but not listed as one.</li>
 * </ul>
 */
public enum UserType {
    HUMAN,
    SYSTEM,
    BOT
}
