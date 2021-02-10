FROM navikt/java:15
LABEL maintainer="Team Farskapsportal" \
      email="nav.ikt.prosjekt.og.forvaltning.farskapsportal@nav.no"

# Ref https://doc.nais.io/clusters/gcp/#starting-application-when-istio-proxy-is-ready
COPY --from=redboxoss/scuttle:latest /scuttle /bin/scuttle
ENV ENVOY_ADMIN_API=http://127.0.0.1:15000
ENV ISTIO_QUIT_API=http://127.0.0.1:15020
ENTRYPOINT ["scuttle", "node", "index.js"]

ADD ./target/farskapsportal-api-*.jar app.jar

EXPOSE 8080
