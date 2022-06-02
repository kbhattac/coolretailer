#
# UI
#
FROM nginx:alpine
RUN apk update
RUN rm -rf /usr/share/nginx/html/*
COPY static/nginx.conf /etc/nginx/nginx.conf
COPY static/lib /usr/share/nginx/html/lib
COPY static/ux.* /usr/share/nginx/html/
EXPOSE 80/tcp