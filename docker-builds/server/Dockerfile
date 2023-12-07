FROM ethlance-base:latest as build_server

WORKDIR $ETHLANCE_SOURCE_ROOT

## Smart contracts
RUN cat shared/src/ethlance/shared/smart_contracts_${ETHLANCE_ENV}.cljs

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
# COPY --from=build_server /build/ethlance/resources/public/contracts /deploy/ui/contracts/
# COPY --from=build_server /build/ethlance/ui/resources/public/* /deploy/ui/
CMD [ "node", "ethlance_server.js" ]
