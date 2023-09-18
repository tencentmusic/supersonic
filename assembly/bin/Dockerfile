FROM node:lts
LABEL maintainer="supersonic"
ENV sbinDir=$(cd $(dirname "$0"); pwd)
ENV baseDir=$(cd $sbinDir/.. && pwd -P)
ENV runtimeDir=$baseDir/../runtime
ENV buildDir=$baseDir/build
WORKDIR /app
COPY $baseDir/../launchers/standalone/target/*.tar.gz ${buildDir}/supersonic-standalone.tar.gz
RUN cd $baseDir \
    && rm -fr ${buildDir}/*.tar.gz && rm -fr dist && mvn -f $baseDir/../ clean package -DskipTests \
COPY $baseDir/../launchers/standalone/target/*.tar.gz ${buildDir}/supersonic-standalone.tar.gz

FROM node:lts
ENV requirementPath=$baseDir/../chat/core/src/main/python/requirements.txt
RUN cd $buildDir && tar xvf supersonic-webapp.tar.gz && mv supersonic-webapp webapp
COPY webapp ../../launchers/standalone/target/classes
RUN pip install -r ${requirementPath} && echo "install python modules success" && mkdir -p ${runtimeDir}  && rm -fr $runtimeDir/*
WORKDIR /web

COPY ../../webapp .

ARG REACT_APP_BASE_URL

RUN npm config set registry https://registry.npm.taobao.org && \
npm install && \
npm run build && \
npm install -g serve

ENTRYPOINT [ "serve", "-s", "build" ]
CMD [ "-l", "3214" ]
