FROM debian:stretch-slim
MAINTAINER "Filip Bielejec" <filip@district0x.io>

RUN apt-get update -y \
    && apt-get install --no-install-recommends -y \
    -q wget nginx

# replace nginx config
COPY docker-builds/ui/nginx.conf /etc/nginx/nginx.conf

# replace default server
COPY docker-builds/ui/default /etc/nginx/sites-available/default

# nginx config
COPY docker-builds/ui/ethlance.com /etc/nginx/sites-available/ethlance.com

# setup error page
RUN wget --no-check-certificate -O X0X.html https://raw.githubusercontent.com/district0x/X0X/master/X0X.html \
  && mv X0X.html /usr/share/nginx/html/X0X.html

# setup static server
RUN ln -s -f /etc/nginx/sites-available/ethlance.com /etc/nginx/sites-enabled/ethlance.com

# get compiled JS
COPY resources/public /ethlance/resources/public/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
