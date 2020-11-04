
FROM ubuntu:18.04

RUN apt-get update -y && \
    apt-get install -y --no-install-recommends apache2 apache2-dev curl mysql-client ack-grep vim bash \
    bash-completion less build-essential git libfcgi-dev ca-certificates apt-utils git openssh-client less iproute2 \
    procps curl wget unzip nano jq lsb-release ca-certificates apt-transport-https dialog gnupg2 libc6 libgcc1 \
    libgssapi-krb5-2 libicu[0-9][0-9] liblttng-ust0 libstdc++6 zlib1g  libz-dev zlib1g-dev locales sudo zip && \
    rm -rf /var/lib/apt/lists/* && \
    a2enmod cgid && a2enmod rewrite && \
    git clone https://github.com/FastCGI-Archives/mod_fastcgi.git && \
    cd mod_fastcgi && \
    cp Makefile.AP2 Makefile && \
    make top_dir=/usr/share/apache2 && \
    make top_dir=/usr/share/apache2 install && \
    sed -i -e 's,LogLevel warn,LogLevel debug,' /etc/apache2/apache2.conf

ENV SDKMAN_DIR=/usr/local/sdkman
RUN  curl -s "https://get.sdkman.io" | bash && \
    echo "sdkman_auto_answer=true" > $SDKMAN_DIR/etc/config && \
    echo "sdkman_auto_selfupdate=false" >> $SDKMAN_DIR/etc/config && \
    echo "sdkman_insecure_ssl=true" >> $SDKMAN_DIR/etc/config && \
    bash -c ". $SDKMAN_DIR/bin/sdkman-init.sh && sdk install java 20.2.0.r11-grl"
ENV JAVA_HOME=${SDKMAN_DIR}/candidates/java/current

RUN ${JAVA_HOME}/bin/gu install native-image
ENV PATH=${PATH}:${JAVA_HOME}/bin

COPY fastcgi.conf /etc/apache2/conf-enabled/
COPY default_index.html /var/www/html/index.html
EXPOSE 80

CMD ["apachectl", "-D", "FOREGROUND"]