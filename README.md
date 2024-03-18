# Limo Control Center

The Limo Control Center is a web application that allows you to control the Limo robot in a 3D map of the scanned
environment.

## Requirements

* A **Redis database** is required to store the robot's position and the map. You will be able to configure host, port,
  username and password in the configuration file.
* Java 17
* Node.js 17 or later with npm

## How build / use

Note that there is a `compose.yml` file for Docker Compose, but it doesn't work yet, for exemple I still have issues
forwarding the Redis port to the server. Help is welcome.

Once everything above is set up, you can:

#### Directly run the server from the command line

```shell
cd KotlinServer
./gradlew runEverything
```

#### Build a jar file

```shell
cd KotlinServer
./gradlew webAndServerJar
```

The jar file will be located in `KotlinServer/build/libs`.

#### Run the jar file

To run fat jar ("fat" because it contains everything: dependencies and web files), you only need the Redis database and
Java 17 or later.

```shell
java -jar path/to/jarfile.jar
```

## VueJs GUI (vue-gui)

If you want to run the GUI alone, you can do so by following the instructions below.

### Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

Personnaly I just use IntelliJ IDEA with the Vue.js plugin, it works very well.

**Type Support for `.vue` Imports in TS**: TypeScript cannot handle type information for `.vue` imports by default, so 
we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript 
language service aware of `.vue` types.

### Project Setup for the GUI alone

#### Install Dependencies

```sh
npm install
```

#### Compile and Hot-Reload for Development

```sh
npm run dev
```

#### Type-Check, Compile and Minify for Production

```sh
npm run build
```
