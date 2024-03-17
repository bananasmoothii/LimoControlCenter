FROM node:20 as vueBuild
COPY --chown=node:node vue-gui /home/node/src
WORKDIR /home/node/src
RUN npm install
RUN npm run build

FROM gradle:8-jdk17 AS gradleBuild
COPY --chown=gradle:gradle KotlinServer /home/gradle/src
WORKDIR /home/gradle/src/
# same action as in build.gradle.kts: copy the dist folder to the resources folder
COPY --from=vueBuild /home/node/src/dist ./build/resources/main/webstatic
RUN gradle shadowJar --no-daemon

FROM openjdk:17
EXPOSE 80
RUN mkdir /app
COPY --from=gradleBuild /home/gradle/src/build/libs/*.jar /app/limo-control-center.jar
ENTRYPOINT ["java", "-jar", "/app/limo-control-center.jar", "--dockerized"]