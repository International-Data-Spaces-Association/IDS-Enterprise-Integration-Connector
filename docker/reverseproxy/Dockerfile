FROM nginx:mainline-alpine
RUN nginx -v
RUN mkdir /etc/cert

COPY nginx.conf /etc/nginx
COPY server.crt /etc/cert/server.crt
COPY server.key /etc/cert/server.key
