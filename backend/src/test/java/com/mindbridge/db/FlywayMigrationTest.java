package com.mindbridge.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfig.class)
class FlywayMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void usersTableExistsAfterMigration() {
        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_name = 'users'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void allMigrationsSucceeded() {
        Integer failed = jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE success = false",
            Integer.class);
        assertThat(failed).isZero();
    }

    @Test
    void emailUniquenessIsCaseInsensitive() {
        jdbc.update("INSERT INTO users (email, password_hash) VALUES ('Test@Example.com', 'x')");
        try {
            org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DuplicateKeyException.class,
                () -> jdbc.update("INSERT INTO users (email, password_hash) VALUES ('test@example.com', 'y')"));
        } finally {
            jdbc.update("DELETE FROM users WHERE lower(email) = 'test@example.com'");
        }
    }
}
