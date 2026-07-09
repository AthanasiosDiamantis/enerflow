package de.saki.enerflow.core.service;

/**
 * Thrown when attempting to create a user with a username that is already taken.
 *
 * @author saki
 */
public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super("Username already exists: " + username);
    }
}