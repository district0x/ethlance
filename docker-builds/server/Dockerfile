ARG DOCKER_REGISTRY
FROM $DOCKER_REGISTRY/cljs-web3-ci:node-20.18.1 AS builder

ARG ETHLANCE_ENV
ARG ETHLANCE_DEPLOY_SEED
USER root

# Set environment variables
ENV ETHLANCE_SOURCE_ROOT=/build/ethlance \
    ETHLANCE_ENV=$ETHLANCE_ENV \
    BUILD_ENV=$ETHLANCE_ENV
ENV ETHLANCE_SERVER_ROOT=${ETHLANCE_SOURCE_ROOT}/server \
    ETHLANCE_DEPLOY_SEED=${ETHLANCE_DEPLOY_SEED} \
    SMART_CONTRACTS_BUILD_PATH="${ETHLANCE_SOURCE_ROOT}/resources/public/contracts/build" \
    SMART_CONTRACTS_PATH="${ETHLANCE_SOURCE_ROOT}/shared/src/ethlance/shared/smart_contracts_${ETHLANCE_ENV}.cljs" \
    ETHLANCE_CONFIG_PATH="${ETHLANCE_SOURCE_ROOT}/config/server-config-${ETHLANCE_ENV}.edn" \
    UI_CONFIG_PATH="${ETHLANCE_SOURCE_ROOT}/config/ui-config-${ETHLANCE_ENV}.edn"

WORKDIR ${ETHLANCE_SOURCE_ROOT}
COPY . .
COPY ethlance-config/config  "${ETHLANCE_SOURCE_ROOT}/config"

# Clone required libraries
RUN git clone --depth 1 https://github.com/district0x/d0x-libs /build/d0x-libs

# Compile contracts
RUN yarn install && ETHLANCE_ENV="${ETHLANCE_ENV}" npx truffle compile

# Build UI
WORKDIR "${ETHLANCE_SOURCE_ROOT}/ui"
RUN yarn && \
    ETHLANCE_ENV="${ETHLANCE_ENV}" npx shadow-cljs release dev-ui && \
    ./node_modules/less/bin/lessc resources/public/less/main.less resources/public/css/main.css --verbose

# Build server
WORKDIR "${ETHLANCE_SOURCE_ROOT}/server"
RUN yarn && npx shadow-cljs release dev-server

# Final stage - minimal runtime image
FROM node:20-slim
ARG ETHLANCE_ENV
ENV ETHLANCE_ENV=$ETHLANCE_ENV \
    ETHLANCE_SOURCE_ROOT=/build/ethlance
ENV ETHLANCE_SERVER_ROOT=/build/ethlance/server \
    SMART_CONTRACTS_BUILD_PATH="${ETHLANCE_SOURCE_ROOT}/resources/public/contracts/build" \
    SMART_CONTRACTS_PATH="${ETHLANCE_SOURCE_ROOT}/shared/src/ethlance/shared/smart_contracts_${ETHLANCE_ENV}.cljs" \
    UI_CONFIG_PATH="/build/ethlance/config/ui-config-prod.edn" \
    ETHLANCE_CONFIG_PATH="/build/ethlance/config/server-config-prod.edn"

WORKDIR ${ETHLANCE_SERVER_ROOT}

COPY --from=builder /build /build
# Initialize events log
RUN echo -n "{:last-processed-block 27409455}" > ethlance-events.log
RUN pm2 install pm2-logrotate

EXPOSE 6300
CMD ["npx", "pm2-runtime", "out/ethlance_server.js"]
