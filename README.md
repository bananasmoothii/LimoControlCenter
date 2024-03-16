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


## VueJs GUI (vue-gui)

This template should help get you started developing with Vue 3 in Vite.

### Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

Personnaly I just use IntelliJ IDEA with the Vue.js plugin.

**Type Support for `.vue` Imports in TS**: TypeScript cannot handle type information for `.vue` imports by default, so 
we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript 
language service aware of `.vue` types.

### Project Setup for the GUI alone

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

#### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
