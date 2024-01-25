ARG BUILD_ENV=qa
FROM --platform=amd64 487920318758.dkr.ecr.us-west-2.amazonaws.com/cljs-web3-ci:node-18.17.0
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

## Deploy Smart contracts
WORKDIR /build/ethlance
# RUN yarn && ETHLANCE_ENV=qa npx truffle migrate --network ethlance.mad.is-testnet --reset
