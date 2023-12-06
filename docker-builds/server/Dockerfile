ARG BUILD_ENV=qa
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


FROM init as build_server

## Smart contracts
WORKDIR /build/ethlance
RUN yarn && ETHLANCE_ENV=qa npx truffle compile 
#migrate --network ethlance.mad.is-testnet --reset

# Release deployment
WORKDIR $ETHLANCE_SOURCE_ROOT/ui 
RUN yarn && \
    ETHLANCE_ENV=qa npx shadow-cljs release dev-ui && \
    ./node_modules/less/bin/lessc resources/public/less/main.less resources/public/css/main.css --verbose

# Release deployment
WORKDIR ${ETHLANCE_SERVER_ROOT}
RUN yarn && npx shadow-cljs -A:local-deps release dev-server

FROM node:18
ENV ETHLANCE_SOURCE_ROOT=/deploy
ENV ETHLANCE_SERVER_ROOT=/deploy/server
ENV ETHLANCE_ENV=qa
ENV DEPLOY_TARGET=/deploy
WORKDIR ${ETHLANCE_SERVER_ROOT}

COPY --from=build_server /build/ethlance/resources /deploy/resources
COPY --from=build_server /build/ethlance/server/node_modules /deploy/server/node_modules/
COPY --from=build_server /build/ethlance/server/package.json /build/ethlance/server/out/ethlance_server* /deploy/server/
COPY --from=build_server /build/ethlance/resources/public/contracts /deploy/ui/contracts/
COPY --from=build_server /build/ethlance/ui/resources/public/* /deploy/ui/
CMD [ "node", "ethlance_server.js" ]
