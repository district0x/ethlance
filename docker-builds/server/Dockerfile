FROM --platform=amd64 487920318758.dkr.ecr.us-west-2.amazonaws.com/cljs-web3-ci:node-18.17.0 as init
ARG BUILD_ENV=qa
USER root

ENV ETHLANCE_COMPONENT="server"
ENV ETHLANCE_SOURCE_ROOT=/build/ethlance
ENV ETHLANCE_SERVER_ROOT=/build/ethlance/server
ENV ETHLANCE_ENV=$BUILD_ENV
ENV DEPLOY_TARGET=/deploy/

ENV SMART_CONTRACTS_PATH="/build/ethlance/shared/src/ethlance/shared/smart_contracts_${BUILD_ENV}.cljs"
ENV SMART_CONTRACTS_BUILD_PATH="/build/ethlance/resources/public/contracts/build"
ENV ETHLANCE_CONFIG_PATH="/build/ethlance/config/${ETHLANCE_COMPONENT}-config-${BUILD_ENV}.edn"

WORKDIR /build/ethlance
COPY . .

RUN cat "${SMART_CONTRACTS_PATH}"

WORKDIR /build/ethlance-config
COPY config .

WORKDIR /build
# RUN git clone https://github.com/district0x/d0x-libs

FROM init as build_server
WORKDIR $ETHLANCE_SOURCE_ROOT
RUN yarn install && ETHLANCE_ENV="${BUILD_ENV}" npx truffle compile

# Release deployment
WORKDIR $ETHLANCE_SOURCE_ROOT/ui 
RUN yarn && \
    ETHLANCE_ENV="${BUILD_ENV}" npx shadow-cljs release dev-ui && \
    ./node_modules/less/bin/lessc resources/public/less/main.less resources/public/css/main.css --verbose

# Release deployment
WORKDIR "${ETHLANCE_SOURCE_ROOT}/server"
RUN yarn && npx shadow-cljs release dev-server

FROM node:18
ENV ETHLANCE_SOURCE_ROOT=/deploy
ENV ETHLANCE_SERVER_ROOT=/deploy/server
ENV ETHLANCE_ENV=qa
ENV DEPLOY_TARGET=/deploy
WORKDIR "${ETHLANCE_SERVER_ROOT}"

COPY --from=build_server /build/ethlance/resources /deploy/resources
COPY --from=build_server /build/ethlance/server/node_modules /deploy/server/node_modules/
COPY --from=build_server /build/ethlance/server/package.json /build/ethlance/server/out/ethlance_server* /deploy/server/
COPY --from=build_server /build/ethlance/resources/public/contracts /deploy/ui/contracts/
COPY --from=build_server /build/ethlance/ui/resources/public/* /deploy/ui/
CMD [ "node", "ethlance_server.js" ]