# Limo Control Center

The Limo Control Center is a web application that allows you to control the Limo robot in a 3D map of the scanned
environment.

## How build / use

**Important:** The Python file only works with **ros1**. Using ros2 should be possible with some modifications in the
python code only.

First, clone the repository in all Limo robots. In the [Python_limo](./Python_limo) folder, install the requirements:

```shell
sudo pip install -r requirements.txt
sudo apt install tmux # use for the launch.sh file
```

There is a [launch.sh](./Python_limo/launch.sh) file that you can use to launch the robot's main node, the lidar and
the pathfinder.

Run the launch.sh file and launch the python file:

```shell
sudo chmod +x launch.sh # make the file executable
./launch.sh
python3 main.py --map # remove the --map argument if you don't want to send the map to the server
```

---

Then, you can run the server in the KotlinServer folder.

### First method: use Docker (easier and less breakable but slow)

**Requirements:**

* Docker
* Docker-compose

For windows, see the [Docker Desktop](https://docs.docker.com/desktop/install/windows-install/) installation guide.

#### Run the server

```shell
docker-compose up --build
```

The `--build` option is useful only if you want to rebuild the image (if the code changed).

### Second method: run the server directly (do this if you want to modify the server code)

**Requirements:**

* A **Redis database** is required to store the robot's position and the map. You will be able to configure host, port,
  username and password in the configuration file.
* Java 17
* Node.js 17 or later with npm

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

<details>
<summary>If you want to run the GUI alone, click here</summary>

## VueJs GUI (vue-gui)

If you want to run the GUI alone, you can do so by following the instructions below.

Note that in development mode, the GUI will try to connect to the server at port 80. For exemple, the Kotlin server
runs on `localhost:80` and npm's dev mode runs on `localhost:5173`, but it still tries to connect to `localhost:80`.

### Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and
disable Vetur).

Personnaly I just use IntelliJ IDEA with the Vue.js plugin, it works very well.

**Type Support for `.vue` Imports in TS**: TypeScript cannot handle type information for `.vue` imports by default, so
we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we
need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript
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

</details>