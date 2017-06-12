Intro
=====
Unit tests, which do not rely on a Vault server being available, are separated from
these integration tests, which do require a Vault instance.

Running the Integration Tests
=============================
Originally this test suite required a decent amount of manual setup.  You had to run and configure a Vault server 
instance on your machine, and populate several environment variables with values that would be picked up by the 
tests.

Since then, the tests have been modified to work with [TestContainers](https://www.testcontainers.org/), a Java 
library that efficiently manages Docker containers and makes them available to JUnit tests.  So now, setup of the 
Vault server instance is entirely automated, and dealt with by the test suite itself.

However, to run these tests you do need to have a current version of Docker installed on your machine.  This is 
supported for Linux, OS X, and Windows, although the details vary significantly between those operating systems.
See the [Docker website](https://www.docker.com/) for information on installing Docker on your OS, after checking 
also with the TestContainers website for OS-specific caveats (Windows in particular).

With Docker installed on your machine, you can run this test suite using the `integrationTest` Gradle task:

`$ ./gradlew integrationTest`
