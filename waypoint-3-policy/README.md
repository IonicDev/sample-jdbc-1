# Ionic Java SDK Sample Application / JDBC / Postgresql / Policy

The [Ionic SDK](https://dev.ionic.com/) provides an easy-to-use interface to the
[Ionic Platform](https://www.ionic.com/). In particular, the Ionic SDK exposes functions to perform Key Management
and Data Encryption.

In a previous post, a sample application demonstrated the ability to mark protected data (via the protection key) with
user-specified attributes.  It is possible to update the policy for a user's Ionic tenant keyspace, such that key
release decisions can consider these attributes.

Users interact with the Ionic platform through a designated tenant.  Ionic tenants are analogous to virtual servers,
and act as logical containers for the data associated with an organization.  All authenticated / authorized users
associated with the organization are able to share access to its cryptography keys, and thus by extension the data
protected by those keys.

When a user wants to view protected data, a key request is sent to the tenant server.  This request identifies the
client requesting the key to unlock the protected data.  The tenant software then validates the request against the
Ionic policies in effect for the tenant.

Multiple policies may be evaluated in the context of a key request.  This allows the server to make a fine-grained
decision whether the key should be included in the response.

The Ionic platform allows for authorized users to update policies programmatically, via a set of web APIs.  The
policies included in a web request might add specify new conditions for key release, or update / delete existing
conditions.  The web API user must authenticate the request using an account that has the *API Administrator* role.  An
overview of the policy API is available [here](https://dev.ionic.com/api/policies).

## Sample Ionic Policy

This sample will consider two distinct Ionic data policies.  The first policy protects data that is marked with the
attribute *classification=pii*, instructing the Ionic server to deny any key request that is so marked.

```json
{
  "enabled": true,
  "policyId": "jdbc-sample-pii",
  "description": "Data is marked with classification matching 'pii'.",
  "ruleCombiningAlgId": "deny-overrides",
  "target": {
    "condition": {
      "functionId": "string-at-least-one-member-of",
      "args": [
        {
          "dataType": "string",
          "value": [
            "pii"
          ]
        },
        {
          "category": "resource",
          "id": "classification"
        }
      ]
    }
  },
  "rules": [
    {
      "effect": "Deny",
      "description": "Always deny."
    }
  ]
}
```

This is a simple data policy.  A more fine-grained policy could restrict key access to users/devices associated with a
specific organizational role, or to individual users/devices.

The second policy protects data that is marked with the attribute *department=HR*, instructing the Ionic server to deny
any key request that is so marked.

```json
{
  "enabled": true,
  "policyId": "jdbc-sample-department-HR",
  "description": "Data is marked with department matching 'HR'.",
  "ruleCombiningAlgId": "deny-overrides",
  "target": {
    "condition": {
      "functionId": "string-at-least-one-member-of",
      "args": [
        {
          "dataType": "string",
          "value": [
            "HR"
          ]
        },
        {
          "category": "resource",
          "id": "department"
        }
      ]
    }
  },
  "rules": [
    {
      "effect": "Deny",
      "description": "Always deny."
    }
  ]
}
```

Typically, policies would permit access to protected data by virtue of an organizational role (for example, allow the
role *HR* to access data marked with *pii*).  The simplicity of this policy is for demonstration purposes.

This sample application is designed to behave differently with regard to these two data policies.  The attribute *pii*
is marked on sensitive data columns.  The attribute *department* is marked on all rows (records), and is equal to the
department associated with the record.  This allows the policies to be used in conjunction with each other.  Access to
a data field might be guarded both its type (*pii*) or by its instance data (*department*).

## Prerequisites

- the 24-digit tenant ID associated with the organization's Ionic tenant
- the account name and password of an Ionic tenant account that has been granted the *API Administrator* role

The other prerequisites for this version of the sample application are unchanged from those of the
[previous version](../waypoint-2-attributes/README.md).

It is assumed that the previous version of the sample application is set up as described in the corresponding
instructions.

## Ionic JDBC Sample Application Content

Much of the content for this version of the sample is the same as the previous version.  There are a few differences:

**[javasdk-sample-jdbc/pom.xml]**

The JSON library used by the Ionic SDK has been added as a dependency of the sample application.  (This is to enable
the client to parse the server response payload for the policy create request.

```xml
        <dependency>
            <groupId>org.glassfish</groupId>
            <artifactId>javax.json</artifactId>
            <version>1.0.4</version>
        </dependency>
```

**[javasdk-sample-jdbc/src/main/java/com/ionic/sdk/addon/jdbc/IonicResultSetHandler.java]**

This class has been enhanced to track the data returned for each row from the JDBC table read.  One of the sample
policies denies access to any Ionic-protected data field associated with a record with a certain department value.  If
decryption is denied for all protected data fields, the row is filtered from the result set.

**[javasdk-sample-jdbc/src/main/java/com/ionic/sdk/addon/policy/PolicyService.java]**

This class provides a means to publish policies associated with this sample application to the relevant Ionic tenant.
The test case demonstrates how visibility to the protected result set changes as the policies are published.

The class requires as input the following:
- the 24-digit tenant ID associated with the organization's tenant
- the account name and password of a tenant account that has been granted the *API Administrator* role

**[javasdk-sample-jdbc/src/test/resources/test.properties.xml]**

```xml
    <entry key='ionic.tenant'>555555555555555555555555</entry>  <!--substitute your Ionic tenant id here-->
    <entry key='ionic.user'>myuser@ionic.com</entry>  <!--substitute your Ionic tenant account here-->
    <entry key='ionic.password'>mypassword</entry>  <!--substitute your Ionic tenant account password here-->
```

The tenant ID, account name, and account password associated with a tenant *API Administrator* are specified here.  If
the specified user is not granted the *API Administrator* role, the request will fail.

**[javasdk-sample-jdbc/src/test/resources/ionic/policy.pii.json]**
**[javasdk-sample-jdbc/src/test/resources/ionic/policy.dept.json]**

These two resources are hard-coded Ionic policies that are used to demonstrate dynamic access to data in the context
of the sample application test case.

**[javasdk-sample-jdbc/src/test/java/com/ionic/sdk/addon/jdbc/test/CreateReadJdbcTest.java]**

This class method "ReadRecords" has been enhanced to read the table content several times.  In between each read, Ionic
server policy is modified.

```java
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
```

- The first Ionic policy request adds the *DENY pii* policy to the active policy set.
- The second Ionic policy request adds the *DENY department=HR* policy to the active policy set.
- The third Ionic policy request removes the *DENY pii* policy from the active policy set.
- The fourth Ionic policy request removes the *DENY department=HR* policy from the active policy set.

## Sample Application Walk-through

1. Start PostgreSQL server (as detailed in [previous walk-through](../waypoint-1-base/README.md)).

1. Substitute the JSON text of your Ionic Secure Enrollment Profile into the file
    **[javasdk-sample-jdbc/src/test/resources/ionic/ionic.sep.plaintext.json]**.

1. Edit the text file **[javasdk-sample-jdbc/src/test/resources/test.properties.xml]**.  Substitute the values of the
   following configuration settings with those appropriate for your Ionic tenant.

    ```xml
    <entry key='ionic.tenant'>555555555555555555555555</entry>  <!--substitute your Ionic tenant id here-->
    <entry key='ionic.user'>myuser@ionic.com</entry>  <!--substitute your Ionic tenant account here-->
    <entry key='ionic.password'>mypassword</entry>  <!--substitute your Ionic tenant account password here-->
    ```

1. Navigate to the *waypoint-3-policy* folder of the sample-jdbc-1 repository.  Run the following command to 
package the (updated) sample application:
    ```shell
    mvn clean package
    ```

The standard output of the test case execution shows how the behavior of the system changes as the effective Ionic
policy changes.

```shell
sample-jdbc-1\waypoint-3-policy>mvn clean package
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building Ionic Java SDK Sample Application, JDBC usage 0.0.3-SNAPSHOT
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
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 1 John Williams 35762 Engineering
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 2 Thomas Johnson 57198 Marketing
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 3 Elizabeth Jackson 55100 HR
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest testJdbc_2_ReadRecords APPLY POLICY 'RESTRICT PII'
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 3
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 1 [RESTRICTED] [RESTRICTED] 35762 Engineering
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 2 [RESTRICTED] [RESTRICTED] 57198 Marketing
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 3 [RESTRICTED] [RESTRICTED] 55100 HR
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest testJdbc_2_ReadRecords APPLY POLICY 'RESTRICT HR'
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 2
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 1 [RESTRICTED] [RESTRICTED] 35762 Engineering
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 2 [RESTRICTED] [RESTRICTED] 57198 Marketing
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest testJdbc_2_ReadRecords REMOVE POLICY 'RESTRICT PII'
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 2
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 1 John Williams 35762 Engineering
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 2 Thomas Johnson 57198 Marketing
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest testJdbc_2_ReadRecords REMOVE POLICY 'RESTRICT HR'
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 3
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 1 John Williams 35762 Engineering
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 2 Thomas Johnson 57198 Marketing
INFO com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest readRecords 3 Elizabeth Jackson 55100 HR
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.891 s - in com.ionic.sdk.addon.jdbc.test.CreateReadJdbcTest
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------

sample-jdbc-1\waypoint-3-policy>
```

The output after the line *testJdbc_2_ReadRecords READ ALL RECORDS* shows that the result set contains three rows,
and that all data is viewable.

After the *RESTRICT PII* policy is applied, a table read shows three rows, but the substitution of the text
**[RESTRICTED]** in place in the first and last names.  This corresponds to the application of the rule restricting
access to column data marked with the *pii* attribute.

After the *RESTRICT HR* policy is applied, a table read shows two rows.  As none of the Ionic-protected data in
Elizabeth Jackson's record is viewable, the class **IonicResultSetHandler** filtered that row from the result set data.

After the *RESTRICT PII* policy is removed, a table read shows two rows.  The *pii* data is now viewable.

After the *RESTRICT HR* policy is removed, a table read shows three rows.  The Ionic policy state now matches the
state at the beginning of the test case method.

# Conclusion

This set of articles demonstrates some of the data protection capabilities of the Ionic platform.  The platform allows
for the seamless encryption of arbitrary structured data.  Attributes may be applied to the protection of each piece of
data.  Policy may be programmatically manipulated to alter the view of protected data.  Additional controls are
available to further define how access to data is allowed.

Ionic's platform is powerful and flexible enough to be broadly applicable to the data protection needs of modern
organizations.
