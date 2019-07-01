# Ionic Java SDK Sample Application / JDBC / Postgresql

The [Ionic SDK](https://dev.ionic.com/) provides an easy-to-use interface to the
[Ionic Platform](https://www.ionic.com/). In particular, the Ionic SDK exposes functions to perform Key Management
and Data Encryption.

The Ionic SDK allows users to apply encryption to both unstructured data (individual files) and to structured data held
in relational databases.  The use case for structured data typically involves the encryption of some fields in one or
more database tables.  These fields may contain personally identifiable information (PII) or other data considered
sensitive.  The logic to apply Ionic protection to this data may be resident anywhere in the software stack of the
application.  The key point is that, at rest, the sensitive data is stored in the database in an encrypted form, and
that Ionic platform controls are used to guard the release of the encryption key.

This sample application will demonstrate a simple use case involving the Ionic protection of certain attributes of a
single database table.  The [PostgreSQL](https://www.postgresql.org/) open source object-relational database system
will be used to store the data. The [Apache Commons DbUtils](https://commons.apache.org/proper/commons-dbutils/)
library will be used to broker access to the sample database table.  The sample code takes the form of a
[JUnit](https://junit.org/junit4/) test class, which will insert a row into the database, and then read the table
content back out of the database.  The JUnit framework is chosen to demonstrate the Ionic capabilities with minimal
needed setup.

## Prerequisites

- physical machine (or virtual machine) with the following software installed
  - [Git](https://git-scm.com/) distributed version control system 
  - Java Runtime Environment 7+ (either
    [OpenJDK JRE](https://openjdk.java.net/install/index.html) or
    [Oracle JRE](https://www.oracle.com/technetwork/java/javase/downloads/index.html))
  - [Apache Maven](https://maven.apache.org/) (Java software project management tool)
- a valid [Ionic Secure Enrollment Profile](https://dev.ionic.com/getting-started/create-ionic-profile) (a plaintext
json file containing access token data)

During the walk-through of this sample application, you will download the following:
- the PostgreSQL binary
- the git repository associated with this sample application

## Ionic JDBC Sample Application

In order to run the sample application, a running PostgreSQL database instance is needed.  A short PSQL command will
then be executed to create the table used by the sample.  The table contains the following columns:

- ID
- First Name
- Last Name
- Zip Code
- Department

When the sample is run, two test cases are executed (in order):

- first, three new records are crafted from embedded random data, Ionic protection is applied, and the records are
inserted into the database table
- second, all rows are read from the table, unwrapping the Ionic protection as needed, and the content is written to
the console

We will also run a PSQL command to display the content of the table, to demonstrate the Ionic protection applied to the
sensitive data fields while in the database.

## Ionic JDBC Sample Application Content

Let's take a brief tour of the content of this sample application.

**[javasdk-sample-jdbc/pom.xml]**

Maven is a commonly used build tool that will be used to assemble the sample.

Here we declare the dependencies for the project.

```xml
    <dependencies>
        <dependency>
            <groupId>com.ionic</groupId>
            <artifactId>ionic-sdk</artifactId>
            <version>2.5.0</version>
        </dependency>
        <dependency>
            <groupId>commons-dbutils</groupId>
            <artifactId>commons-dbutils</artifactId>
            <version>1.7</version>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.2.5</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
```

Note that the PostgreSQL JDBC driver (42.2.5) is a test dependency.  This sample app uses the Java Database
Connectivity (JDBC) interface to interact with the data source.  The main application logic need not know the flavor of
database with which it is interacting.  This sample would work equally well with any JDBC driver implementation, by
substituting the appropriate dependency declaration here, and by tweaking the SQL command that creates the table.

**[javasdk-sample-jdbc/src/test/resources/jdbc/create.table.txt]**

This SQL command will be used to create the table used by the sample.

**[javasdk-sample-jdbc/src/test/resources/ionic/ionic.sep.plaintext.json]**

The template JSON in this file should be replaced with the text of the Ionic Secure Enrollment Profile to be used.  The
SEP contains configuration specifying the Ionic server to use, as well as data to identify the client making the
requests.

**[javasdk-sample-jdbc/src/test/resources/logging.properties]**

This file specifies the configuration of Java's logging facility.  This facility will be used when the sample runs
to display the content of the database table.

**[javasdk-sample-jdbc/src/test/resources/test.properties.xml]**

This file contains configuration settings for the sample.

- **ionic-profile** - the classpath location of the Ionic Secure Enrollment Profile (SEP)
- **ionic-columns** - the database columns that contain data to be protected by the Ionic platform
- **driverClassName** - the class implementing the JDBC *Driver* interface
- **jdbc.url** - the URL specifying the URL address of the database to be used
- **jdbc.user** - the database user account to be used by the sample
- **jdbc.password** - the password of the database user account to be used by the sample
- **jdbc.sql.insert** - the PreparedStatement SQL that will be used to insert records into the database
- **jdbc.sql.select** - the PreparedStatement SQL that will be used to read all records from the database

**[javasdk-sample-jdbc/src/main/java/com/ionic/sdk/addon/jdbc/RowSet.java]**

This Java class contains a very simple object implementation that can be used to hold the content of a database
ResultSet.

**[javasdk-sample-jdbc/src/main/java/com/ionic/sdk/addon/jdbc/IonicResultSetHandler.java]**

This Java class implements the *Apache DbUtils* interface *ResultSetHandler*.  When an SQL SELECT is called, the data
is returned in one or more ResultSet objects.  This implementation iterates through the rows returned in the first
ResultSet, adding either the value, or the decrypted value, based on whether the value comes from an Ionic-protected
column.

**[javasdk-sample-jdbc/src/test/java/com/ionic/sdk/addon/jdbc/test/CreateReadJdbcTest.java]**

```java
public void setUp() throws Exception {
```
The setup function runs before each test.  It reads the test configuration properties, and initializes the Ionic
*Agent* object instance.

```java
public final void testJdbc_1_CreateRecordInTable() throws SQLException, ReflectiveOperationException, IonicException {
```
The first test case opens a connection to the database, then crafts new records and inserts them into the sample
database table.  The test configuration specifies database parameters.  The Ionic class *ChunkCipherV2* will be used to
encrypt record fields as needed.  The fields to be Ionic-protected are specified in the property "ionic-columns".  The
data to be used is randomized from lists of common names.

Note that an Ionic-encrypted text string will be somewhat larger than the original plaintext string.  It includes a key
ID reference, as well as the base64 representation of the ciphertext, which includes an encryption initialization
vector.  Ensure that the database to hold the Ionic-protected fields is large enough to store the full ciphertext
string, which would be somewhat larger than the original text.

```java
public final void testJdbc_2_ReadRecordsInTable() throws SQLException, ReflectiveOperationException {
```
The second test case opens a connection to the database, then reads all records from the sample database table.  The
*IonicResultSetHandler* object handles the seamless decryption of any Ionic-protected fields in ResultSet.

## Sample Application Walk-through

*Note: This walk-through was done on an English Microsoft Windows OS.  The console output snippets contain OS and 
locale commands and characters specific to this environment.  Please substitute the appropriate commands and characters 
as needed for your environment.*

1. Download [PostgreSQL image](https://www.enterprisedb.com/download-postgresql-binaries).  (This walk-through was
prepared using version 10.8 of the Postgres software.)

1. Inflate image into an empty folder on your filesystem.

1. Open a *Command Prompt* window.  Navigate to the root folder of the unzipped PostgreSQL instance.
   1. Run the following command to create the sample database.  (You will be prompted twice to enter/reenter a
        superuser password for the database.)
        ```shell
        bin\initdb.exe -D data -U postgres -W -E UTF8 -A scram-sha-256
        ```

        ```shell
        postgresql\pgsql>bin\initdb.exe -D data -U postgres -W -E UTF8 -A scram-sha-256
        The files belonging to this database system will be owned by user "demouser".
        This user must also own the server process.

        The database cluster will be initialized with locale "English_United States.1252".
        The default text search configuration will be set to "english".

        Data page checksums are disabled.

        Enter new superuser password:
        Enter it again:

        creating directory data ... ok
        creating subdirectories ... ok
        selecting default max_connections ... 100
        selecting default shared_buffers ... 128MB
        selecting dynamic shared memory implementation ... windows
        creating configuration files ... ok
        running bootstrap script ... ok
        performing post-bootstrap initialization ... ok
        syncing data to disk ... ok

        Success. You can now start the database server using:

            bin/pg_ctl -D data -l logfile start


        postgresql\pgsql>
        ```
   1. Run the following command to start the PostgreSQL server.
        ```shell
        bin\pg_ctl.exe -D data start
        ```
        ```shell
        postgresql\pgsql>bin\pg_ctl.exe -D data start
        waiting for server to start....
        listening on IPv4 address "127.0.0.1", port 5432
        listening on IPv6 address "::1", port 5432
        ...
        database system is ready to accept connections
         done
        server started

        postgresql\pgsql>
        ```

1. Open a second *Command Prompt* window.  Navigate to the root folder of the unzipped PostgreSQL instance.
   1. Run the following command to enter the PSQL command console.
        ```shell
        bin\psql.exe -U postgres
        ```
        ```shell
        postgresql\pgsql>bin\psql.exe -U postgres
        Password for user postgres:
        psql (10.8)
        WARNING: Console code page (437) differs from Windows code page (1252)
                 8-bit characters might not work correctly. See psql reference
                 page "Notes for Windows users" for details.
        Type "help" for help.

        postgres=#
        ```

   1. Check the PostgreSQL version (this also verifies connectivity to the server process).
        ```shell
        postgres=# SELECT version();
                             version
        ------------------------------------------------------------
        PostgreSQL 10.8, compiled by Visual C++ build 1800, 64-bit
        (1 row)

        postgres=#
        ```

   1. Create the sample table in the database by performing a copy/paste operation from the sample resource
   *create.table.txt* into the Command Prompt window.
        ```shell
        postgres=# CREATE TABLE personnel(
        postgres(# id serial PRIMARY KEY,
        postgres(# first VARCHAR (64),
        postgres(# last VARCHAR (64),
        postgres(# zip VARCHAR (64),
        postgres(# department VARCHAR (64));
        CREATE TABLE
        postgres=#
        ```

1. Open a third *Command Prompt* window.  Clone the git sample application repository into an empty folder on your
filesystem.
    ```shell
    git clone https://github.com/IonicDev/sample-jdbc-1.git
    ```

1. Substitute the JSON text of your Ionic Secure Enrollment Profile into the file
    **[sample-jdbc-1/waypoint-1-base/src/test/resources/ionic/ionic.sep.plaintext.json]**.

1. In the third Command Prompt window, navigate to the *waypoint-1-base* folder of the sample-jdbc-1 repository. Run 
the following command to package the sample application (which will also execute the sample's unit test):
    ```shell
    mvn clean package
    ```
    ```shell
    sample-jdbc-1\waypoint-1-base>mvn clean package

    [INFO] ------------------------------------------------------------------------
    [INFO] Building Ionic Java SDK Sample Application, JDBC usage 0.0.1-SNAPSHOT
    [INFO] ------------------------------------------------------------------------
    ...
    [INFO] -------------------------------------------------------
    [INFO]  T E S T S
    [INFO] -------------------------------------------------------
    [INFO] Running com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest
    INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest testJdbc_1_CreateRecords 1
    INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest testJdbc_1_CreateRecords 1
    INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest testJdbc_1_CreateRecords 1
    INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest testJdbc_2_ReadRecords READ ALL RECORDS
    INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 3
    INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 1 Joseph Martinez 80232 HR
    INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 2 Michael Taylor 70344 Marketing
    INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 3 Barbara Garcia 73239 Engineering
    [INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.257 s - in com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest
    ...
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time: 4.469 s
    [INFO] Final Memory: 14M/309M
    [INFO] ------------------------------------------------------------------------

    sample-jdbc-1\waypoint-1-base>
    ```

1. In the second Command Prompt window, enter the following command to view the database table content:
    ```sql
    select * from personnel;
    ```
    ```shell
    postgres=# select * from personnel;
     id |                       first                       |                        last                         |                      zip                      | department
    ----+---------------------------------------------------+-----------------------------------------------------+-----------------------------------------------+-------------
      1 | ~!2!D7GH9fQ4ihk!z5kKEW/rdn1C6AG9FvD0m2U+cc5A4w!   | ~!2!D7GH9HQ-i-I!xhu6InmrXnhzVK4lbz37+jcfe4ex!       | ~!2!D7GH9PQ8i0s!VFo7gBKs6t8TpmtDn7nYoFIMViQ8! | HR
      2 | ~!2!D7GHDAhRInQ!XajJts8ZCsZ0V8ro+EdKLw4MYsvRmow!  | ~!2!D7GHDYhXI48!MfAjBEdrkAfcuhGCDplm4pnZiwUBK+FV7w! | ~!2!D7GHDQhVIyY!OwrDdOJBeeH7J6F7Tq9DaATrqr6W! | Marketing
      3 | ~!2!D7GHDogrOoE!HqvSz8Rbe+pq/Sy7MCo8qM+fAIGYJb0!  | ~!2!D7GHDggpOig!fOGrrxzF+AeFruntqqiUe12VVWH6!       | ~!2!D7GHD4gvO9M!eIlCdbfN2eRjytID8uhk2aVLr3VO! | Engineering
    (1 row)

    postgres=#
    ```

# Conclusion

In this sample application, we used the Ionic platform and SDK in order to protect data elements marked as sensitive
with easily reversible AES encryption.  The encryption keys are protected by the Ionic platform, and released only on
an authorized request, based on the Ionic policy in force at the time of the request.

More sophisticated implementations might batch Ionic platform key operations to more efficiently process result sets.
They might also apply certain attributes to the keys associated with an encryption, which could be used in the policy
decision at the time of the key request.

Ionic's platform is powerful and flexible enough to be broadly applicable to the data protection needs of modern
organizations.
