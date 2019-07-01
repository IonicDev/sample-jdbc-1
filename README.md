# Ionic Java SDK Sample Application / JDBC

The [Ionic SDK](https://dev.ionic.com/) provides an easy-to-use interface to the
[Ionic Platform](https://www.ionic.com/). In particular, the Ionic SDK exposes functions to perform Key Management
and Data Encryption.

This git repository iterates through a series of waypoints, demonstrating an Ionic SDK integration with a simple
JDBC (Java Database Connectivity) application.

### [Waypoint 1](./waypoint-1-base/README.md)

The first waypoint introduces a simple JDBC test case, involving a single newly defined database table.  The *Apache
DbUtils* library is used to wrap read access to the database.  The *PostgreSQL* database management system application
is used as the data store for the test case.  The *JUnit* library is used within the *Apache Maven* Java software
project management tool as the framework for running the test case.

### [Waypoint 2](./waypoint-2-attributes/README.md)

The second waypoint introduces
[ChunkCryptoEncryptAttributes](https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.4.0/com/ionic/sdk/agent/cipher/chunk/data/ChunkCryptoEncryptAttributes.html).
Ionic APIs allow the specification of key attributes that are associated with cryptography keys.  These attributes are
used to mark the data being encrypted, and to contribute to a key release policy decision at the time a data decryption
is requested.

### [Waypoint 3](./waypoint-3-policy/README.md)

The third waypoint introduces the use of Ionic policy to control the decryption of protected data elements.  Two
policies are used, each protecting an aspect of the data being protected.

- The *pii* policy protects data columns that are marked with the attribute *pii*.
- The *department* policy protects data rows that are marked with a given department identifier.

These policies may be used individually, or in concert with each other.
