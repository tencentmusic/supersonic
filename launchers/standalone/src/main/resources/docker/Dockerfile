FROM docker-images.music.woa.com/di_base_img/tme_di_base_centos:8_7
MAINTAINER admin
ADD ../../../../target/launchers-standalone-*.gz /app/
RUN ls -l /app/launchers-standalone-*/bin/
ENV TZ=Asia/Shanghai
ENV LANG C.UTF-8
EXPOSE 9082
ENTRYPOINT ["sh","-c","/app/launchers-standalone-0.6-SNAPSHOT/bin/run.sh && tail -f /dev/null"]