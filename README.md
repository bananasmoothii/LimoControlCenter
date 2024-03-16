# Limo Control Center

The Limo Control Center is a web application that allows you to control the Limo robot in a 3D map of the scanned
environment.

## How to use

1. Set up the Redis database

2. Use `gradlew shadowJar` to build the jar file

   Note: on windows, use `./gradlew.bat shadowJar`

3. Run the jar file once to create the configuration file and edit it to set the redis server address (and password if
   necessary)

4. Run the jar file again to start the server