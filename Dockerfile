
#FROM redboxoss/scuttle:latest AS scuttle
FROM ghcr.io/navikt/baseimages/temurin:17-appdynamics
LABEL maintainer="Team Farskapsportal" \
      email="nav.ikt.prosjekt.og.forvaltning.farskapsportal@nav.no"

ARG JAR_PATH
ENV JAVA_OPTS=$JAVA_OPTS
COPY apps/api/target/app.jar app.jar
EXPOSE 8080

# Ref https://doc.nais.io/clusters/gcp/#starting-application-when-istio-proxy-is-ready
#ENV ENVOY_ADMIN_API=http://127.0.0.1:15000
#ENV ISTIO_QUIT_API=http://127.0.0.1:15020
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
#ENTRYPOINT ["scuttle", "/dumb-init", "--", "/entrypoint.sh"]

