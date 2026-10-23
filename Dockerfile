# O estágio de build roda na arquitetura de quem builda ($BUILDPLATFORM): o .jar é igual em
# qualquer CPU, então compilar nativo e só o estágio final sair em arm64 (EC2 t4g) evita rodar
# o Maven emulado pelo QEMU, que levaria dezenas de minutos.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# auth-core primeiro: o app depende dela via ~/.m2. Camadas separadas para o cache do
# go-offline sobreviver a mudanças só no código.
COPY auth-core/pom.xml auth-core/pom.xml
COPY auth-core/src auth-core/src
RUN mvn -B -q -f auth-core/pom.xml install -DskipTests

COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-jammy
# O FusoHorarioConfig ja ajusta a JVM, mas so depois que o contexto sobe. Definir aqui
# tambem faz os logs de startup sairem no fuso certo, em vez de UTC.
ENV TZ=America/Sao_Paulo
WORKDIR /app
RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
