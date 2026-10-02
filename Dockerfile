FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

COPY src/ src/

RUN ./mvnw -B -ntp clean package -DskipTests \
    && cp target/portal-solicitacoes-backend-*.jar /workspace/application.jar

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system app \
    && useradd --system --gid app --create-home app

WORKDIR /app

COPY --from=build --chown=app:app /workspace/application.jar application.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]