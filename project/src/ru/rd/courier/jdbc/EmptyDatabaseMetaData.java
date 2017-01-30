/*
 * Copyright 2005-2017 Courier AUTHORS: please see AUTHORS file.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY AUTHORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package ru.rd.courier.jdbc;

import java.sql.*;

public class EmptyDatabaseMetaData implements DatabaseMetaData {
    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getDatabaseMajorVersion() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getDatabaseMinorVersion() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getDriverMajorVersion() {
        throw new UnsupportedOperationException();
    }

    public int getDriverMinorVersion() {
        throw new UnsupportedOperationException();
    }

    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String types[]) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getDefaultTransactionIsolation() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getJDBCMajorVersion() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getJDBCMinorVersion() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxBinaryLiteralLength() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxCatalogNameLength() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxCharLiteralLength() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxColumnNameLength() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxColumnsInGroupBy() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxColumnsInIndex() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxColumnsInOrderBy() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxColumnsInSelect() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxColumnsInTable() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxConnections() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxCursorNameLength() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxIndexLength() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxProcedureNameLength() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxRowSize() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxSchemaNameLength() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxStatementLength() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxStatements() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxTableNameLength() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxTablesInSelect() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getMaxUserNameLength() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getResultSetHoldability() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getSQLStateType() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean allProceduresAreCallable() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean allTablesAreSelectable() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean isCatalogAtStart() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean isReadOnly() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean locatorsUpdateCopy() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean nullPlusNonNullIsNull() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean nullsAreSortedAtEnd() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean nullsAreSortedAtStart() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean nullsAreSortedHigh() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean nullsAreSortedLow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean storesLowerCaseIdentifiers() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean storesMixedCaseIdentifiers() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean storesUpperCaseIdentifiers() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsANSI92FullSQL() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsBatchUpdates() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsColumnAliasing() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsConvert() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsCoreSQLGrammar() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsCorrelatedSubqueries() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsExpressionsInOrderBy() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsExtendedSQLGrammar() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsFullOuterJoins() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsGetGeneratedKeys() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsGroupBy() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsGroupByBeyondSelect() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsGroupByUnrelated() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsLikeEscapeClause() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsLimitedOuterJoins() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsMinimumSQLGrammar() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsMultipleOpenResults() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsMultipleResultSets() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsMultipleTransactions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsNamedParameters() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsNonNullableColumns() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsOrderByUnrelated() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsOuterJoins() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsPositionedDelete() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsPositionedUpdate() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsSavepoints() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsSchemasInDataManipulation() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsSelectForUpdate() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsStatementPooling() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public RowIdLifetime getRowIdLifetime() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getClientInfoProperties() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsStoredProcedures() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsSubqueriesInComparisons() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsSubqueriesInExists() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsSubqueriesInIns() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsTableCorrelationNames() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsTransactions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsUnion() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsUnionAll() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean usesLocalFilePerTable() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean usesLocalFiles() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean deletesAreDetected(int type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean insertsAreDetected(int type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean othersDeletesAreVisible(int type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean othersInsertsAreVisible(int type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean ownDeletesAreVisible(int type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean ownInsertsAreVisible(int type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsResultSetHoldability(int holdability) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsResultSetType(int type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean updatesAreDetected(int type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsConvert(int fromType, int toType) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getCatalogSeparator() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getCatalogTerm() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getDatabaseProductName() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getDatabaseProductVersion() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getDriverName() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getDriverVersion() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getExtraNameCharacters() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getIdentifierQuoteString() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getNumericFunctions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getProcedureTerm() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getSQLKeywords() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getSchemaTerm() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getSearchStringEscape() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getStringFunctions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getSystemFunctions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getTimeDateFunctions() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getURL() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getUserName() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Connection getConnection() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getCatalogs() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getSchemas() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getTableTypes() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getTypeInfo() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getCrossReference(String primaryCatalog, String primarySchema, String primaryTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    public ResultSet getPseudoColumns(String catalog,
                         String schemaPattern,
                         String tableNamePattern,
                         String columnNamePatter)
                           throws SQLException
    {
        throw new UnsupportedOperationException();        
    }

    public boolean generatedKeyAlwaysReturned() throws SQLException
    {
        throw new UnsupportedOperationException();        
    }
}
