
#FROM redboxoss/scuttle:latest AS scuttle
FROM navikt/java:15
LABEL maintainer="Team Farskapsportal" \
      email="nav.ikt.prosjekt.og.forvaltning.farskapsportal@nav.no"

#COPY --from=scuttle /scuttle /bin/scuttle
COPY ./target/farskapsportal-api-*.jar app.jar
EXPOSE 8080

# Ref https://doc.nais.io/clusters/gcp/#starting-application-when-istio-proxy-is-ready
#ENV ENVOY_ADMIN_API=http://127.0.0.1:15000
#ENV ISTIO_QUIT_API=http://127.0.0.1:15020
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
#ENTRYPOINT ["scuttle", "/dumb-init", "--", "/entrypoint.sh"]

