FROM openjdk:21-jdk-bullseye as base

# Set the working directory in the container
WORKDIR /usr/src/app

# Argument to pass in the supersonic version at build time
ARG SUPERSONIC_VERSION

# Copy the supersonic standalone zip file into the container
COPY assembly/build/supersonic-standalone-${SUPERSONIC_VERSION}.zip .

# Unzip the supersonic standalone zip
RUN unzip supersonic-standalone-${SUPERSONIC_VERSION}.zip && \
    rm supersonic-standalone-${SUPERSONIC_VERSION}.zip

FROM openjdk:21-slim

# Set the working directory in the container
WORKDIR /usr/src/app

# Argument to pass in the supersonic version at build time
ARG SUPERSONIC_VERSION

# Copy the supersonic standalone folder into the container
COPY --from=base /usr/src/app/supersonic-standalone-${SUPERSONIC_VERSION} ./supersonic-standalone-${SUPERSONIC_VERSION}

# Create a symbolic link to the supersonic installation directory
RUN ln -s /usr/src/app/supersonic-standalone-${SUPERSONIC_VERSION} /usr/src/app/supersonic-standalone-latest

# Set the working directory to the supersonic installation directory
WORKDIR /usr/src/app/supersonic-standalone-${SUPERSONIC_VERSION}

# Expose the default port
EXPOSE 9080
# Command to run the supersonic daemon
RUN chmod +x bin/supersonic-daemon.sh
CMD ["bash", "-c", "bin/supersonic-daemon.sh restart standalone ${S2_DB_TYPE} && tail -f /dev/null"]
