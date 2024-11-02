#!/usr/bin/env bash
# Exit immediately if a command exits with a non-zero status
set -e
VERSION=$1

# Image name
IMAGE_NAME="supersonicbi/supersonic"

# Default tag is latest
TAGS="latest"

# If VERSION is provided, add it to TAGS and tag the image as latest
if [ -n "$VERSION" ]; then
  TAGS="$TAGS $VERSION"
  docker tag $IMAGE_NAME:$VERSION $IMAGE_NAME:latest
fi

# Push Docker images
for TAG in $TAGS; do
  echo "Pushing Docker image $IMAGE_NAME:$TAG"
  docker push $IMAGE_NAME:$TAG
done

echo "Docker images pushed successfully."