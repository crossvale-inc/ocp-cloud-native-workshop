FROM registry.access.redhat.com/ubi8/nginx-122

# Add application sources to a directory that the assemble script expects them
# and set permissions so that the container runs without root access
USER 0
ADD index.html /tmp/src/

RUN chown -R 1001:0 /tmp/src
USER 1001

# Let the assemble script to install the dependencies
RUN /usr/libexec/s2i/assemble

ADD nginx-proxy.conf ${NGINX_DEFAULT_CONF_PATH}/nginx-proxy.conf

# Run script uses standard ways to run the application
CMD /usr/libexec/s2i/run