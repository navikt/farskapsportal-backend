FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-26-dev AS tools
USER root
RUN apk add --no-cache glibc-locale-nb

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-26
LABEL maintainer="Team Farskapsportal" \
      email="nav.ikt.prosjekt.og.forvaltning.farskapsportal@nav.no"

COPY --from=tools /bin/busybox /bin/sh
COPY --from=tools /bin/busybox /bin/printenv
# The dev image's BusyBox needs libcrypt, which is absent from the minimal runtime.
COPY --from=tools /usr/lib/libcrypt.so.1.1.0 /usr/lib/libcrypt.so.1.1.0
COPY --from=tools /usr/lib/libcrypt.so.1 /usr/lib/libcrypt.so.1
COPY --from=tools /usr/lib/locale/ /usr/lib/locale/

WORKDIR /app
COPY apps/api/target/app.jar app.jar
EXPOSE 8080

ENV LANG=nb_NO.UTF-8 LANGUAGE='nb_NO:nb' LC_ALL=nb_NO.UTF-8 TZ="Europe/Oslo"

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar"]
CMD ["app.jar"]