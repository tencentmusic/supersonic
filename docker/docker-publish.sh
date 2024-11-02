#!/bin/bash
# 确保脚本在出错时退出
set -e
# 镜像名称
IMAGE_NAME="supersonicbi/supersonic"

# 默认标签为 latest
TAGS=("latest")

# 如果有 Git 标签，则使用 Git 标签作为额外的镜像标签
if [ -n "$GITHUB_REF" ]; then
  GIT_TAG=$(echo $GITHUB_REF | sed 's/refs\/tags\///')
  TAGS+=("$GIT_TAG")
fi

# 推送 Docker 镜像
for TAG in "${TAGS[@]}"; do
  echo "Pushing Docker image $IMAGE_NAME:$TAG"
  docker push $IMAGE_NAME:$TAG
done

echo "Docker images pushed successfully."