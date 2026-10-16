package com.djk.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * 통합 테스트용 MySQL. docker-compose.yml 과 같은 이미지·설정에 docs/schema.sql 을 그대로 적용한다.
 * ddl-auto=validate 가 실제 스키마로 엔티티를 검증하도록 H2 대신 MySQL 을 쓴다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestContainer {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("djk")
                .withEnv("TZ", "Asia/Seoul")
                .withCommand(
                        "--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_unicode_ci",
                        "--default-time-zone=+09:00")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("docs/schema.sql"),
                        "/docker-entrypoint-initdb.d/01-schema.sql");
    }
}
