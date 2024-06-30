# Use an official OpenJDK runtime as a parent image
FROM openjdk:8-jdk

# Set the working directory in the container
WORKDIR /usr/src/app

# Argument to pass in the supersonic version at build time
ARG SUPERSONIC_VERSION

# Install the vim editor.
RUN apt-get update && \
    apt-get install -y vim && \
    rm -rf /var/lib/apt/lists/*

# Copy the supersonic standalone zip file into the container
COPY assembly/build/supersonic-standalone-${SUPERSONIC_VERSION}.zip .

# Unzip the supersonic standalone zip
RUN unzip supersonic-standalone-${SUPERSONIC_VERSION}.zip && \
    rm supersonic-standalone-${SUPERSONIC_VERSION}.zip

# Set the working directory to the supersonic installation directory
WORKDIR /usr/src/app/supersonic-standalone-${SUPERSONIC_VERSION}

# Expose the default port
EXPOSE 9080
# Command to run the supersonic daemon
RUN chmod +x bin/supersonic-daemon.sh
CMD ["bash", "-c", "bin/supersonic-daemon.sh restart prd && tail -f /dev/null"]