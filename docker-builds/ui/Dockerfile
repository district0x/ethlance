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

# Set working directory and prepare source files
WORKDIR ${ETHLANCE_SOURCE_ROOT}
COPY . .
COPY ethlance-config/config  "${ETHLANCE_SOURCE_ROOT}/config"

# Clone required dependencies
RUN git clone --depth 1 https://github.com/district0x/d0x-libs /build/d0x-libs

# Build stage - compile contracts and UI
# Compile contracts
RUN yarn install && ETHLANCE_ENV="${ETHLANCE_ENV}" npx truffle compile

# Build UI components
WORKDIR ${ETHLANCE_SOURCE_ROOT}/ui
RUN yarn install && \
    ETHLANCE_ENV="${ETHLANCE_ENV}" npx shadow-cljs release dev-ui && \
    ./node_modules/less/bin/lessc resources/public/less/main.less resources/public/css/main.css --verbose

# Final stage - using nginx to serve the UI
FROM nginx:alpine
ARG ETHLANCE_ENV
ENV ETHLANCE_ENV=$ETHLANCE_ENV

# Configure nginx
COPY docker-builds/ui/ethlance.conf /etc/nginx/conf.d/
COPY docker-builds/ui/default.conf /etc/nginx/conf.d/default.conf
COPY docker-builds/ui/nginx.conf /etc/nginx/nginx.conf

# Copy built assets from builder stage
COPY --from=builder /build/ethlance/resources /deploy/resources
COPY --from=builder /build/ethlance/resources/public/contracts /deploy/ui/contracts/
COPY --from=builder /build/ethlance/ui/resources/public /deploy/ui/
COPY --from=builder /build/ethlance/config /deploy/config
EXPOSE 80
