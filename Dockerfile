FROM navikt/java:14
LABEL maintainer="Team Farskapsportal" \
      email="nav.ikt.prosjekt.og.forvaltning.farskapsportal@nav.no"

ADD ./target/farskapsportal-api-*.jar app.jar

EXPOSE 8080
