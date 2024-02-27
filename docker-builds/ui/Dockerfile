FROM --platform=amd64 487920318758.dkr.ecr.us-west-2.amazonaws.com/cljs-web3-ci:node-18.17.0 as init
ARG BUILD_ENV=qa
USER root

ENV ETHLANCE_COMPONENT="ui"
ENV ETHLANCE_SOURCE_ROOT=/build/ethlance
ENV ETHLANCE_SERVER_ROOT=/build/ethlance/server
ENV ETHLANCE_ENV=$BUILD_ENV
ENV DEPLOY_TARGET=/deploy/

ENV SMART_CONTRACTS_PATH="/build/ethlance/shared/src/ethlance/shared/smart_contracts_${BUILD_ENV}.cljs"
ENV SMART_CONTRACTS_BUILD_PATH="/build/ethlance/resources/public/contracts/build"
ENV ETHLANCE_CONFIG_PATH="/build/ethlance/config/${ETHLANCE_COMPONENT}-config-${BUILD_ENV}.edn"

WORKDIR /build/ethlance
COPY . .

RUN cat shared/src/ethlance/shared/smart_contracts_${ETHLANCE_ENV}.cljs

WORKDIR /build/ethlance-config
COPY config .

# WORKDIR /build
# RUN git clone https://github.com/district0x/d0x-libs


FROM init as build_stage
ENV ETHLANCE_ENV=qa
WORKDIR $ETHLANCE_SOURCE_ROOT
RUN yarn install && ETHLANCE_ENV="${BUILD_ENV}" npx truffle compile

WORKDIR $ETHLANCE_SOURCE_ROOT/ui
RUN yarn install && \
    ETHLANCE_ENV="${BUILD_ENV}" npx shadow-cljs release dev-ui && \
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