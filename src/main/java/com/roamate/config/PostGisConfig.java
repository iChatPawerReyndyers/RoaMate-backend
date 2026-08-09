package com.roamate.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Hibernate Spatial (org.hibernate.orm:hibernate-spatial) auto-registers the
 * `geometry` type for JTS Point/Polygon fields once it's on the classpath -
 * see MemberLocation, Destination, BeaconAlert. The actual GIST indexes live
 * in the Flyway migration V2__postgis_indexes.sql, not in Java, since they're
 * a physical storage concern PostGIS handles at the SQL level.
 */
@Configuration
@EntityScan(basePackages = "com.roamate")
@EnableJpaRepositories(basePackages = "com.roamate")
public class PostGisConfig {
}
