FROM openjdk:8-jdk-alpine

# Install OpenJFX
RUN apk --no-cache add ca-certificates wget && \
    wget --quiet --output-document=/etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
    wget https://github.com/sgerrand/alpine-pkg-java-openjfx/releases/download/8.151.12-r0/java-openjfx-8.151.12-r0.apk && \
    apk add --no-cache java-openjfx-8.151.12-r0.apk shadow

# Install git
RUN apk update && apk upgrade && \
    apk add --no-cache git

# Clone JDime
RUN git clone https://github.com/se-passau/jdime.git

# Move to JDime
WORKDIR jdime
RUN ./gradlew installDist

RUN mkdir /wkdir
WORKDIR /wkdir


ENTRYPOINT ["/jdime/build/install/JDime/bin/JDime"]
