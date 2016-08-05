Intro
=====
Unit tests, which do not rely on a Vault server being available, are separated from
these integration tests, which do require a Vault instance.  

The Vault instance is provisioned automatically as a Docker container, using the 
[TestContainers library](http://testcontainers.viewdocs.io/testcontainers-java/).  
Follow that library's documentation for getting your environment set up to run 
these tests.