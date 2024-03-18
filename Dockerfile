FROM node:20 as vueBuild
COPY --chown=node:node vue-gui /home/node/src
WORKDIR /home/node/src
RUN npm install
RUN npm run build

FROM gradle:8-jdk17 AS gradleBuild
COPY --chown=gradle:gradle KotlinServer /home/gradle/src
WORKDIR /home/gradle/src/
# same action as in build.gradle.kts: copy the dist folder to the resources folder, but here we copy directly to src
# because gradle seems to clean the resources folder before building the jar
RUN rm -rf src/main/resources/webstatic
COPY --from=vueBuild /home/node/src/dist src/main/resources/webstatic
RUN gradle shadowJar --no-daemon

FROM openjdk:17
EXPOSE 80
RUN mkdir /app
COPY --from=gradleBuild /home/gradle/src/build/libs/*.jar /app/limo-control-center.jar
ENTRYPOINT ["java", "-jar", "/app/limo-control-center.jar", "--dockerized"]