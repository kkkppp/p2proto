package org.p2proto.ddl;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DDLExecutor {

    private final DataSource dataSource;

    @Autowired
    public DDLExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Executes all generated DDL statements. The transaction should be
     * handled outside (e.g., via @Transactional on the caller).
     */
    @Transactional
    public List<String> executeDDL(DDLCommand command) throws SQLException, DatabaseException {
        List<String> executed = new ArrayList<>();
        // Obtain the existing transaction-bound connection, if any
        Connection connection = DataSourceUtils.getConnection(dataSource);

        // We will close the Statement, but not the Connection itself.
        // The transaction manager will properly commit/rollback/close.
        try (Statement stmt = connection.createStatement()) {

            Database database = DatabaseFactory
                    .getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            // Generate all SQL statements from Liquibase
            List<String> sqlTexts = new ArrayList<>();
            for (SqlStatement liquibaseStmt : command.getChange().generateStatements(database)) {
                Sql[] sqlArray = SqlGeneratorFactory.getInstance().generateSql(liquibaseStmt, database);
                for (Sql sql : sqlArray) {
                    sqlTexts.add(sql.toString());
                }
            }

            for (String sql : sqlTexts) {
                stmt.execute(sql);
                executed.add(sql);
            }

        } finally {
            // Return the Connection to the transaction manager or pool
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
        return executed;
    }
}
