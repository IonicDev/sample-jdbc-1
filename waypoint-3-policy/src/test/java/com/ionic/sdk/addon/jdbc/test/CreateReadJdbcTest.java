package com.ionic.sdk.addon.jdbc.test;

import com.ionic.sdk.addon.jdbc.IonicResultSetHandler;
import com.ionic.sdk.addon.jdbc.RowSet;
import com.ionic.sdk.addon.policy.PolicyService;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.AgentSdk;
import com.ionic.sdk.agent.cipher.chunk.ChunkCipherV2;
import com.ionic.sdk.agent.cipher.chunk.data.ChunkCryptoEncryptAttributes;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.transaction.AgentTransactionUtil;
import com.ionic.sdk.core.res.Resource;
import com.ionic.sdk.device.DeviceUtils;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.device.profile.persistor.ProfilePersistor;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.error.SdkError;
import com.ionic.sdk.json.JsonIO;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.InputStream;
import java.net.URL;
import java.security.Security;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Perform testing of atomic JDBC operations.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateReadJdbcTest {

    /**
     * Class scoped logger.
     */
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Test configuration.
     */
    private final Properties properties = new Properties();

    /**
     * Test Ionic agent, used to protect data on insert into database, and unprotect data on fetch from database.
     */
    private final Agent agent = new Agent();

    /**
     * Set up for each test case to be run.
     *
     * @throws Exception on failure to read the test configuration
     */
    @Before
    public void setUp() throws Exception {
        // load test configuration: "src/test/resources/test.properties.xml"
        final URL urlTestProperties = Resource.resolve("test.properties.xml");
        Assert.assertNotNull(urlTestProperties);
        try (InputStream is = urlTestProperties.openStream()) {
            properties.loadFromXML(is);
        }
        // initialize Ionic agent for use
        if (!agent.isInitialized()) {
            final String ionicProfile = properties.getProperty("ionic-profile");
            Assert.assertNotNull(ionicProfile);
            AgentSdk.initialize(Security.getProvider("SunJCE"));
            final URL urlIonicProfile = Resource.resolve(ionicProfile);
            final ProfilePersistor profilePersistor = new DeviceProfilePersistorPlainText(urlIonicProfile);
            agent.initialize(profilePersistor);
        }
    }

    /**
     * Create a new record in the configured JDBC database table.
     *
     * @throws SQLException                 on failure to access the configured JDBC data source
     * @throws ReflectiveOperationException on failure to instantiate JDBC classes (check test classpath)
     * @throws IonicException               on failure to protect data on database insert
     */
    @Test
    public final void testJdbc_1_CreateRecords() throws SQLException, ReflectiveOperationException, IonicException {
        logger.entering(null, null);
        // test context
        final String dbUser = properties.getProperty("jdbc.user");
        final String dbPassword = properties.getProperty("jdbc.password");
        final String driverClassName = properties.getProperty("driverClassName");
        final String dbUrl = properties.getProperty("jdbc.url");
        final String dbSqlInsert = properties.getProperty("jdbc.sql.insert");
        final String ionicColumnsText = properties.getProperty("ionic-columns");
        final Collection<String> ionicColumns = Util.getIonicColumns(ionicColumnsText);
        // Ionic attributes used to mark protected columns of a new record
        final String ionicAttributesFirst = properties.getProperty("ionic-attributes.first");
        final String ionicAttributesLast = properties.getProperty("ionic-attributes.last");
        final String ionicAttributesZip = properties.getProperty("ionic-attributes.zip");

        // establish database connection
        final Properties properties = new Properties();
        properties.setProperty("user", dbUser);
        properties.setProperty("password", dbPassword);
        final Class<?> driverClass = Class.forName(driverClassName);
        final Driver driver = (Driver) driverClass.newInstance();
        try (Connection connection = driver.connect(dbUrl, properties)) {
            Assert.assertNotNull(connection);
            final ChunkCipherV2 chunkCipher = new ChunkCipherV2(agent);
            final int recordsToCreate = 3;
            for (int i = 0; (i < recordsToCreate); ++i) {
                // perform database table insert
                final String firstName = Util.getFirstName();
                final String lastName = Util.getLastName();
                final String zipCode = Util.getZipCode();
                final String department = Util.getDepartment();
                // mark each protected data column with appropriate attributes
                final ChunkCryptoEncryptAttributes attributesFirst = Util.toEncryptAttributes(ionicAttributesFirst);
                final ChunkCryptoEncryptAttributes attributesLast = Util.toEncryptAttributes(ionicAttributesLast);
                final ChunkCryptoEncryptAttributes attributesZip = Util.toEncryptAttributes(ionicAttributesZip);
                // mark all protected record columns with appropriate attributes
                final ChunkCryptoEncryptAttributes[] attributesArray = {attributesFirst, attributesLast, attributesZip};
                for (ChunkCryptoEncryptAttributes attributes : attributesArray) {
                    attributes.getKeyAttributes().put("department", Collections.singletonList(department));
                }
                // apply sample app business logic to Ionic-protect specified columns
                final String firstNameDb = ionicColumns.contains("first")
                        ? chunkCipher.encrypt(firstName, attributesFirst) : firstName;
                final String lastNameDb = ionicColumns.contains("last")
                        ? chunkCipher.encrypt(lastName, attributesLast) : lastName;
                final String zipCodeDb = ionicColumns.contains("zip")
                        ? chunkCipher.encrypt(zipCode, attributesZip) : zipCode;
                final QueryRunner queryRunner = new QueryRunner();
                final int inserts = queryRunner.update(connection, dbSqlInsert,
                        firstNameDb, lastNameDb, zipCodeDb, department);
                logger.info("" + inserts);
            }
        }
        logger.exiting(null, null);
    }

    /**
     * Read all records in the configured JDBC database table.
     *
     * @throws SQLException                 on failure to access the configured JDBC data source
     * @throws ReflectiveOperationException on failure to instantiate JDBC classes (check test classpath)
     * @throws IonicException               on configuration errors, inability to access test resources, server errors
     */
    @Test
    public final void testJdbc_2_ReadRecords() throws SQLException, ReflectiveOperationException, IonicException {
        logger.entering(null, null);
        // test context
        final String dbUser = properties.getProperty("jdbc.user");
        final String dbPassword = properties.getProperty("jdbc.password");
        final String driverClassName = properties.getProperty("driverClassName");
        final String dbUrl = properties.getProperty("jdbc.url");
        final String dbSqlSelect = properties.getProperty("jdbc.sql.select");
        final String ionicColumnsText = properties.getProperty("ionic-columns");
        final Collection<String> ionicColumns = Util.getIonicColumns(ionicColumnsText);
        final String ionicTenant = properties.getProperty("ionic.tenant");
        final String ionicUser = properties.getProperty("ionic.user");
        final String ionicPassword = properties.getProperty("ionic.password");
        // policy service is used to manipulate Ionic server policies for your tenant
        final URL url = AgentTransactionUtil.getProfileUrl(agent.getActiveProfile());
        final PolicyService policyService = new PolicyService(url, ionicTenant, ionicUser, ionicPassword);
        // establish database connection
        final Properties properties = new Properties();
        properties.setProperty("user", dbUser);
        properties.setProperty("password", dbPassword);
        final Class<?> driverClass = Class.forName(driverClassName);
        final Driver driver = (Driver) driverClass.newInstance();
        try (Connection connection = driver.connect(dbUrl, properties)) {
            Assert.assertNotNull(connection);
            logger.info("READ ALL RECORDS");
            readRecords(connection, dbSqlSelect, ionicColumns);
            logger.info("APPLY POLICY 'RESTRICT PII'");
            final String policyIdPii = policyService.addPolicy(
                    DeviceUtils.read(Resource.resolve(RESOURCE_POLICY_PII)));
            readRecords(connection, dbSqlSelect, ionicColumns);
            logger.info("APPLY POLICY 'RESTRICT HR'");
            final String policyIdDept = policyService.addPolicy(
                    DeviceUtils.read(Resource.resolve(RESOURCE_POLICY_DEPT)));
            readRecords(connection, dbSqlSelect, ionicColumns);
            logger.info("REMOVE POLICY 'RESTRICT PII'");
            policyService.deletePolicy(policyIdPii);
            readRecords(connection, dbSqlSelect, ionicColumns);
            logger.info("REMOVE POLICY 'RESTRICT HR'");
            policyService.deletePolicy(policyIdDept);
            readRecords(connection, dbSqlSelect, ionicColumns);
        }
        logger.exiting(null, null);
    }

    private static final String RESOURCE_POLICY_PII = "ionic/policy.pii.json";
    private static final String RESOURCE_POLICY_DEPT = "ionic/policy.dept.json";

    private void readRecords(final Connection connection, final String dbSqlSelect,
                             final Collection<String> ionicColumns) throws SQLException {
        final QueryRunner queryRunner = new QueryRunner();
        final ResultSetHandler<RowSet> handler = new IonicResultSetHandler(agent, ionicColumns);
        final RowSet rowSet = queryRunner.query(connection, dbSqlSelect, handler);
        logger.info("" + rowSet.size());
        for (Object[] row : rowSet) {
            final StringBuilder buffer = new StringBuilder();
            for (Object cell : row) {
                buffer.append(cell);
                buffer.append(" ");
            }
            logger.info(buffer.toString());
        }
    }

    private static class Util {

        /**
         * Test utility method.
         *
         * @return a random common first name
         */
        private static String getFirstName() {
            // https://www.ssa.gov/oact/babynames/decades/century.html
            final List<String> firstNames = Arrays.asList(
                    "James", "Mary", "John", "Patricia", "Robert",
                    "Jennifer", "Michael", "Linda", "William", "Elizabeth",
                    "David", "Barbara", "Richard", "Susan", "Joseph",
                    "Jessica", "Thomas", "Sarah", "Charles", "Karen");
            Collections.shuffle(firstNames);
            return firstNames.iterator().next();
        }

        /**
         * Test utility method.
         *
         * @return a random common last name
         */
        private static String getLastName() {
            // https://en.wikipedia.org/wiki/List_of_most_common_surnames_in_North_America#United_States_(American)
            final List<String> lastNames = Arrays.asList(
                    "Smith", "Johnson", "Williams", "Brown", "Jones",
                    "Miller", "Davis", "Garcia", "Rodriguez", "Wilson",
                    "Martinez", "Anderson", "Taylor", "Thomas", "Hernandez",
                    "Moore", "Martin", "Jackson", "Thompson", "White");
            Collections.shuffle(lastNames);
            return lastNames.iterator().next();
        }

        /**
         * Test utility method.
         *
         * @return a random 5-digit zip code
         */
        private static String getZipCode() {
            final int zipCode = new Random().nextInt(100000);
            return Integer.toString(zipCode);
        }

        /**
         * Test utility method.
         *
         * @return a random department to be associated with a data record
         */
        private static String getDepartment() {
            Collections.shuffle(DEPARTMENTS);
            final String department = DEPARTMENTS.iterator().next();
            DEPARTMENTS.remove(department);
            return department;
        }

        private static final List<String> DEPARTMENTS = new ArrayList<String>(
                Arrays.asList("Engineering", "HR", "Marketing"));

        /**
         * In this sample app, the columns receiving Ionic protection are stored in the test properties.
         *
         * @return the names of the Ionic-protected columns in the sample database table
         */
        private static Collection<String> getIonicColumns(final String ionicColumns) {
            Assert.assertNotNull(ionicColumns);
            return Arrays.asList(ionicColumns.split("\\|"));
        }

        /**
         * Convert test properties value into a {@link ChunkCryptoEncryptAttributes}, suitable for use with encrypt API.
         *
         * @param value the test property
         * @return the {@link ChunkCryptoEncryptAttributes} representation of the value
         * @throws IonicException on json parse failure
         */
        private static ChunkCryptoEncryptAttributes toEncryptAttributes(final String value) throws IonicException {
            final KeyAttributesMap keyAttributes = toKeyAttributesMap(value);
            return new ChunkCryptoEncryptAttributes(keyAttributes);
        }

        /**
         * Convert test properties value into a {@link KeyAttributesMap}, suitable for inclusion in a
         * {@link ChunkCryptoEncryptAttributes}.
         *
         * @param value the test property
         * @return the {@link ChunkCryptoEncryptAttributes} representation of the value
         * @throws IonicException on json parse failure
         */
        private static KeyAttributesMap toKeyAttributesMap(final String value) throws IonicException {
            final KeyAttributesMap keyAttributesMap = new KeyAttributesMap();
            final String json = (value == null) ? "{}" : value;
            final JsonObject jsonObject = JsonIO.readObject(json, SdkError.ISAGENT_INVALIDVALUE);
            for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                final String key = entry.getKey();
                final JsonValue jsonValue = entry.getValue();
                if (jsonValue instanceof JsonArray) {
                    keyAttributesMap.put(key, toStringList((JsonArray) jsonValue));
                }
            }
            return keyAttributesMap;
        }

        /**
         * Convert a set of json values into a {@link KeyAttributesMap} value.
         *
         * @param jsonArray the json structure from the test properties
         * @return a {@link KeyAttributesMap} value
         */
        private static List<String> toStringList(final JsonArray jsonArray) {
            final List<String> values = new ArrayList<String>();
            for (JsonValue jsonValue : jsonArray) {
                if (jsonValue instanceof JsonString) {
                    values.add(((JsonString) jsonValue).getString());
                }
            }
            return values;
        }
    }
}
