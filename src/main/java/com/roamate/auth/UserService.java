package com.roamate.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    /**
     * Every column across the schema that stores a userId as a plain
     * string (see BaseEntity note: userId was never a real FK, so device
     * ids and now User.id both just live here as VARCHARs). Kept as a
     * table/column pair list so migrateAnonymousData can walk it once.
     */
    private static final List<String[]> USER_ID_COLUMNS = List.of(
            new String[]{"trip_members", "user_id"},
            new String[]{"member_locations", "user_id"},
            new String[]{"activity_sessions", "user_id"},
            new String[]{"expense_participants", "user_id"},
            new String[]{"expense_payments", "payer_user_id"},
            new String[]{"expenses", "created_by_user_id"},
            new String[]{"kitty_deposits", "depositor_user_id"},
            new String[]{"beacon_alerts", "raised_by_user_id"},
            new String[]{"event_log", "origin_user_id"},
            new String[]{"location_notes", "author_user_id"},
            new String[]{"checklist_items", "assigned_to_user_id"},
            new String[]{"checklist_items", "owner_user_id"}
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class UsernameTakenException extends RuntimeException {
        public UsernameTakenException(String username) {
            super("Username '" + username + "' is already taken");
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Invalid username or password");
        }
    }

    /**
     * Creates a new account. If previousUserId is supplied (the caller's
     * old anonymous device id), every row currently attributed to that id
     * is reassigned to the new account's id in the same transaction, so a
     * first-time sign-up doesn't strand the trips/expenses/etc. that
     * person already created anonymously.
     */
    @Transactional
    public User register(String username, String rawPassword, String previousUserId) {
        String normalized = username.trim().toLowerCase();
        try {
            User user = new User();
            user.setUsername(normalized);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user = userRepository.save(user);

            if (previousUserId != null && !previousUserId.isBlank() && !previousUserId.equals(user.getId().toString())) {
                migrateAnonymousData(previousUserId, user.getId().toString());
            }

            return user;
        } catch (DataIntegrityViolationException e) {
            // Race: two registrations for the same username landed
            // concurrently and the unique constraint caught the second one
            // (the existsByUsername check below is a pre-check for the
            // common case, not a guarantee).
            throw new UsernameTakenException(normalized);
        }
    }

    public User login(String username, String rawPassword) {
        String normalized = username.trim().toLowerCase();
        Optional<User> user = userRepository.findByUsername(normalized);
        if (user.isEmpty() || !passwordEncoder.matches(rawPassword, user.get().getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user.get();
    }

    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username.trim().toLowerCase());
    }

    private void migrateAnonymousData(String oldUserId, String newUserId) {
        for (String[] tableAndColumn : USER_ID_COLUMNS) {
            String table = tableAndColumn[0];
            String column = tableAndColumn[1];
            jdbcTemplate.update(
                    "UPDATE " + table + " SET " + column + " = ? WHERE " + column + " = ?",
                    newUserId, oldUserId
            );
        }
    }
}
