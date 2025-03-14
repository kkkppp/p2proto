package org.p2proto.ddl;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.AbstractResourceAccessor;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;


public class DDLGenerator {
    public static void main(String[] args) {
        String jdbcUrl = "jdbc:postgresql://host.docker.internal:5432/platform";
        String username = "platform";
        String password = "qwerty";

        // Dynamically generated JSON changelog (example)
        String jsonChangelog = "";
/*        {
          "databaseChangeLog": [
            {
              "changeSet": {
                "id": "create-person-table",
                "author": "yourname",
                "changes": [
                  {
                    "createTable": {
                      "tableName": "person",
                      "columns": [
                        {
                          "column": {
                            "name": "id",
                            "type": "int",
                            "autoIncrement": true,
                            "constraints": {
                              "primaryKey": true,
                              "nullable": false
                            }
                          }
                        },
                        {
                          "column": {
                            "name": "name",
                            "type": "varchar(255)"
                          }
                        },
                        {
                          "column": {
                            "name": "data",
                            "type": "jsonb"
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;*/

/*        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            // Initialize the database object
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            // Use StringResourceAccessor to provide Liquibase with the in-memory JSON
            AbstractResourceAccessor resourceAccessor = new InMemoryResourceAccessor(
                    Collections.singletonMap("changelog.json", jsonChangelog.getBytes(StandardCharsets.UTF_8))
            );
            // Load Liquibase with the dynamically generated JSON
            Liquibase liquibase = new Liquibase("changelog.json", resourceAccessor, database);

            // Generate the native SQL (DDL statements)
            String sql = liquibase.updateSQL(null); // Generate SQL without executing
            System.out.println("Generated DDL SQL:");
            System.out.println(sql);

        } catch (LiquibaseException | Exception e) {
            e.printStackTrace();
        }*/
    }
}
