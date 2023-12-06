FROM --platform=amd64 487920318758.dkr.ecr.us-west-2.amazonaws.com/cljs-web3-ci:node-18.17.0 as init
USER root

RUN npm i -g truffle@v5.11
ENV ETHLANCE_SOURCE_ROOT=/build/ethlance
ENV ETHLANCE_SERVER_ROOT=/build/ethlance/server
ENV ETHLANCE_ENV=qa
ENV DEPLOY_TARGET=/deploy/

WORKDIR /build/ethlance
COPY . .

WORKDIR /build/ethlance-config
COPY config .

WORKDIR /build
RUN git clone https://github.com/district0x/d0x-libs

FROM init as build_stage

## Smart contracts
WORKDIR /build/ethlance
RUN yarn && ETHLANCE_ENV=qa npx truffle compile 

WORKDIR $ETHLANCE_SOURCE_ROOT/ui
RUN yarn && \
    ETHLANCE_ENV=qa npx shadow-cljs release dev-ui && \
    ./node_modules/less/bin/lessc resources/public/less/main.less resources/public/css/main.css --verbose

FROM nginx:alpine
ENV ETHLANCE_ENV=qa

COPY docker-builds/ui/ethlance.conf /etc/nginx/conf.d/
COPY docker-builds/ui/default.conf /etc/nginx/conf.d/default.conf
COPY docker-builds/ui/nginx.conf /etc/nginx/nginx.conf

COPY --from=build_stage /build/ethlance/resources /deploy/resources
COPY --from=build_stage /build/ethlance/resources/public/contracts /deploy/ui/contracts/
COPY --from=build_stage /build/ethlance/ui/resources/public /deploy/ui/

EXPOSE 80
