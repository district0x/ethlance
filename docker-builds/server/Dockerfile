FROM --platform=amd64 487920318758.dkr.ecr.us-west-2.amazonaws.com/cljs-web3-ci:node-20.18.1 as builder
ARG ETHLANCE_ENV=prod
ARG ETHLANCE_DEPLOY_SEED="sign bachelor state zoo expire boat morning situate scene unveil oven crew"
USER root

ENV ETHLANCE_SOURCE_ROOT=/build/ethlance \
    ETHLANCE_ENV=$ETHLANCE_ENV
ENV ETHLANCE_SERVER_ROOT=${ETHLANCE_SOURCE_ROOT}/server \
    ETHLANCE_DEPLOY_SEED=${ETHLANCE_DEPLOY_SEED} \
    SMART_CONTRACTS_BUILD_PATH="${ETHLANCE_SOURCE_ROOT}/resources/public/contracts/build" \
    SMART_CONTRACTS_PATH="${ETHLANCE_SOURCE_ROOT}/shared/src/ethlance/shared/smart_contracts_${ETHLANCE_ENV}.cljs" \
    ETHLANCE_CONFIG_PATH="${ETHLANCE_SOURCE_ROOT}/config/server-config-${ETHLANCE_ENV}.edn" \
    UI_CONFIG_PATH="${ETHLANCE_SOURCE_ROOT}/config/ui-config-${ETHLANCE_ENV}.edn"

WORKDIR ${ETHLANCE_SOURCE_ROOT}
COPY . .

RUN ls -lah
RUN cat "${SMART_CONTRACTS_PATH}"

# # WORKDIR /build/ethlance-config/
# # COPY ethlance-config .

# # WORKDIR /build
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
# # ENV ETHLANCE_ENV=prod \
# #     ETHLANCE_SOURCE_ROOT=/build/ethlance \
# #     ETHLANCE_SERVER_ROOT=${ETHLANCE_SOURCE_ROOT}/server \
# #     UI_CONFIG_PATH="${ETHLANCE_SOURCE_ROOT}/config/ui-config-${ETHLANCE_ENV}.edn" \
# #     ETHLANCE_CONFIG_PATH="${ETHLANCE_SOURCE_ROOT}/config/server-config-${ETHLANCE_ENV}.edn"

# #     WORKDIR "${ETHLANCE_SERVER_ROOT}"
# #     COPY --from=builder /build /build
# # # COPY --from=build_server /build/ethlance/config/ui-config-${ETHLANCE_ENV}.edn /deploy/resources/config/ui-config-${ETHLANCE_ENV}.edn
# # # COPY --from=build_server /build/ethlance/config/server-config-${ETHLANCE_ENV}.edn /deploy/resources/config/server-config-${ETHLANCE_ENV}.edn
# # # COPY --from=build_server /build/ethlance/resources /deploy/resources
# # # COPY --from=build_server /build/ethlance/server/node_modules /deploy/server/node_modules/
# # # COPY --from=build_server /build/ethlance/server/package.json /build/ethlance/server/out/ethlance_server* /deploy/server/
# # # COPY --from=build_server /build/ethlance/resources/public/contracts /deploy/ui/contracts/
# # # COPY --from=build_server /build/ethlance/ui/resources/public/* /deploy/ui/
# # RUN echo -n "{:last-processed-block 27409455}" > ethlance-events.log
# # CMD [ "node", "out/ethlance_server.js" ]

ENV ETHLANCE_ENV=prod \
    ETHLANCE_SOURCE_ROOT=/build/ethlance \
    ETHLANCE_SERVER_ROOT=/build/ethlance/server \
    SMART_CONTRACTS_BUILD_PATH="${ETHLANCE_SOURCE_ROOT}/resources/public/contracts/build" \
    SMART_CONTRACTS_PATH="${ETHLANCE_SOURCE_ROOT}/shared/src/ethlance/shared/smart_contracts_${ETHLANCE_ENV}.cljs" \
    UI_CONFIG_PATH="/build/ethlance/config/ui-config-prod.edn" \
    ETHLANCE_CONFIG_PATH="/build/ethlance/config/server-config-prod.edn"

WORKDIR ${ETHLANCE_SERVER_ROOT}

# Only copy what's needed for runtime
COPY --from=builder /build /build
# COPY --from=builder /build/ethlance/config/ui-config-prod.edn /build/ethlance/config/ui-config-prod.edn
# COPY --from=builder /build/ethlance/config/server-config-prod.edn /build/ethlance/config/server-config-prod.edn
# COPY --from=builder /build/ethlance/resources /build/ethlance/resources
# COPY --from=builder /build/ethlance/server/out/ethlance_server.js /build/ethlance/server/
# COPY --from=builder /build/ethlance/server/node_modules /build/ethlance/server/node_modules/

# Initialize events log
RUN echo -n "{:last-processed-block 27409455}" > ethlance-events.log

EXPOSE 6300
CMD ["node", "ethlance_server.js"]
