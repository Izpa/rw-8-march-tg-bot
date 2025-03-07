FROM openjdk:11

WORKDIR /

COPY target/app.jar app.jar
EXPOSE 3000

CMD java -jar app.jar