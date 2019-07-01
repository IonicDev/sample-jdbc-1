# Ionic Java SDK Sample Application / JDBC / Postgresql / Ionic Attributes

The [Ionic SDK](https://dev.ionic.com/) provides an easy-to-use interface to the
[Ionic Platform](https://www.ionic.com/). In particular, the Ionic SDK exposes functions to perform Key Management
and Data Encryption.

In a previous post, a sample application introduced a use case using the Ionic Java SDK to interact with a PostgreSQL
database table.  The test class carried out two operations: (1) inserting new records into a database table, and (2)
fetching all of the records from the table.  In each case, configuration dictated the protection of certain fields with
Ionic cryptography keys.

The Ionic platform allows for these keys to be associated with adhoc attributes.  Each key is typically used to protect
a single atom of data (for example, a table cell).  These may be used in later policy
decisions when a key release is requested.  For example, a key might be marked with the name of the corresponding data
field "Last Name".  Subsequent requests for keys marked "Last Name" might be denied to requesters lacking permission to
view "Last Name" data.

Ionic key requests may also include metadata identifying the requester of the key.  These might include the Ionic SDK
version in use, the client application identifier, the client operating system, and even the client device.  Any or all
of these fields may be used by the Ionic key server when making a key release policy decision.  Some data may be
viewable only by HR, while other data may be viewed only on a device associated with the organization's CEO.

This version of the sample application will enhance the previous version, adding support for marking protected data
fields with arbitrary attributes.  A future version will demonstrate the programmatic use of Ionic policy to filter out
data cells and rows that are not viewable by the database requester.

## Prerequisites

The prerequisites for this version of the sample application are unchanged from those of the 
[previous version](../waypoint-1-base/README.md):

- physical machine (or virtual machine) with the following software installed
  - [Git](https://git-scm.com/) distributed version control system 
  - Java Runtime Environment 7+ (either
    [OpenJDK JRE](https://openjdk.java.net/install/index.html) or
    [Oracle JRE](https://www.oracle.com/technetwork/java/javase/downloads/index.html))
  - [Apache Maven](https://maven.apache.org/) (Java software project management tool)
- a valid [Ionic Secure Enrollment Profile](https://dev.ionic.com/getting-started/create-ionic-profile) (a plaintext
json file containing access token data)
- the PostgreSQL binary
- the git repository associated with this sample application

It is assumed that the previous version of the sample application is set up as described in the corresponding
instructions.

## Ionic JDBC Sample Application Content

Much of the content for this version of the sample is the same as the previous version.  There are a few differences:

**[javasdk-sample-jdbc/src/test/resources/test.properties.xml]**

```xml
    <entry key='ionic-columns'>first|last|zip</entry>
    <entry key='ionic-attributes.first'>{ "classification": [ "pii", "first" ] }</entry>
    <entry key='ionic-attributes.last'>{ "classification": [ "pii", "last" ] }</entry>
    <entry key='ionic-attributes.zip'>{ "classification": [ "zip" ] }</entry>
```
The first XML line is copied from the previous version.  This line indicates that three of the columns in the sample
database table are to be treated as sensitive content.

The other lines provide JSON snippets to be used by the code that commits new records to the database table.
The second line indicates Ionic attributes to be associated with the table column "first".  When any data is stored
into this column, it is encrypted with an Ionic AES key.  The corresponding Ionic key request associates the key with
the attribute "classification=[pii, first]".  The third and fourth lines similarly associate attributes with
data stored in the corresponding columns.

**[javasdk-sample-jdbc/src/test/java/com/ionic/sdk/addon/jdbc/test/CreateReadJdbcTest.java]**

This bit of code retrieves the attributes to be associated with each of the table columns.

```java
    @Test
    public final void testJdbc_1_CreateRecordInTable() throws SQLException, ReflectiveOperationException, IonicException {
    ...
        final String ionicAttributesFirst = propertiesTest.getProperty("ionic-attributes.first");
        final String ionicAttributesLast = propertiesTest.getProperty("ionic-attributes.last");
        final String ionicAttributesZip = propertiesTest.getProperty("ionic-attributes.zip");
```

This bit of code performs the data encryption for each field, using an alternate SDK API that associates the specified
attributes with the corresponding encryption key.

```java
            // apply sample app business logic to Ionic-protect specified columns
            final String firstNameDb = ionicColumns.contains("first")
                    ? chunkCipher.encrypt(firstName, toEncryptAttributes(ionicAttributesFirst)) : firstName;
            final String lastNameDb = ionicColumns.contains("last")
                    ? chunkCipher.encrypt(lastName, toEncryptAttributes(ionicAttributesLast)) : lastName;
            final String zipCodeDb = ionicColumns.contains("zip")
                    ? chunkCipher.encrypt(zipCode, toEncryptAttributes(ionicAttributesZip)) : zipCode;
```

There are additional helper functions at the bottom of the Java class that convert the configuration into
ChunkCipherEncryptAttributes objects suitable for use with the SDK *encrypt()* API.

## Sample Application Walk-through

1. Start PostgreSQL server (as detailed in [previous walk-through](../waypoint-1-base/README.md)).

1. Substitute the JSON text of your Ionic Secure Enrollment Profile into the file
    **[javasdk-sample-jdbc/src/test/resources/ionic/ionic.sep.plaintext.json]**.

1. Navigate to the *waypoint-2* folder of the sample-jdbc-1 repository.  Run the following command to package
the (updated) sample application:
    ```shell
    mvn clean package
    ```

# Conclusion

As with the previous lesson, the unit test used the Ionic platform and SDK in order to protect sensitive data elements
with AES encryption.

The AES keys used in the encryption are additionally marked with user-specified attributes.  These attributes can then
be used by the server when a key release is requested.  (For example, a key marked "CEO only" will only be released to
a device associated with the account of the CEO.)

The Ionic platform provides the ability to make fine-grained decisions about who gets to see data, and when and where
this is allowed.
