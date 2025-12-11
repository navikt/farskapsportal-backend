FROM ubuntu:22.04 AS locales
RUN apt-get update && apt-get install -y locales
RUN locale-gen nb_NO.UTF-8 && \
    update-locale LANG=nb_NO.UTF-8 LANGUAGE="nb_NO:nb" LC_ALL=nb_NO.UTF-8

FROM gcr.io/distroless/java25
LABEL maintainer="Team Farskapsportal" \
      email="nav.ikt.prosjekt.og.forvaltning.farskapsportal@nav.no"

COPY --from=busybox:1.35.0-glibc /bin/sh /bin/sh
COPY --from=busybox:1.35.0-glibc /bin/printenv /bin/printenv

ENV JAVA_OPTS=$JAVA_OPTS
COPY apps/api/target/app.jar app.jar
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENV LANG=nb_NO.UTF-8 LANGUAGE='nb_NO:nb' LC_ALL=nb_NO.UTF-8 TZ="Europe/Oslo"

CMD ["app.jar"]