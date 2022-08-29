FROM maven:3.8.6-openjdk-11

RUN mkdir /flowgate-build \
  && mkdir /log
VOLUME /flowgate-build /log
COPY build.sh /
RUN chmod o+x build.sh

WORKDIR /
ENTRYPOINT [ "bash", "build.sh" ]
