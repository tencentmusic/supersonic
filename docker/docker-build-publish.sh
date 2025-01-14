#!/usr/bin/env bash

# Function to execute the build script
execute_build_script() {
  echo "Executing build script: assembly/bin/supersonic-build.sh"
  assembly/bin/supersonic-build.sh
}

# Function to build the Docker image
build_docker_image() {
  local version=$1
  echo "Building Docker image: supersonic:$version"
  docker buildx build --no-cache --platform linux/amd64,linux/arm64 \
                      --build-arg SUPERSONIC_VERSION=$version \
                      -t supersonicbi/supersonic:$version \
                      -t supersonicbi/supersonic:latest \
                      -f docker/Dockerfile \
                      --push .
  if [ $? -ne 0 ]; then
    echo "Docker build failed. Exiting."
    exit 1
  fi
  echo "Docker image supersonic:$version built successfully."
}

# Main script execution
VERSION=$1
if [ -z "$VERSION" ]; then
  echo "Usage: $0 <version>"
  exit 1
fi

execute_build_script
build_docker_image $VERSION