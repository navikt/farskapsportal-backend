
#FROM redboxoss/scuttle:latest AS scuttle
FROM ghcr.io/navikt/baseimages/temurin:17-appdynamics
LABEL maintainer="Team Farskapsportal" \
      email="nav.ikt.prosjekt.og.forvaltning.farskapsportal@nav.no"

ARG JAR_PATH
ENV JAVA_OPTS=$JAVA_OPTS
COPY $JAR_PATH /app/app.jar
EXPOSE 8080

ENV MAVEN_OPTS=" --add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
  --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
  --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
  --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
  --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED "

# Ref https://doc.nais.io/clusters/gcp/#starting-application-when-istio-proxy-is-ready
#ENV ENVOY_ADMIN_API=http://127.0.0.1:15000
#ENV ISTIO_QUIT_API=http://127.0.0.1:15020
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
#ENTRYPOINT ["scuttle", "/dumb-init", "--", "/entrypoint.sh"]

