FROM ethlance-base:latest as build_ui
ENV ETHLANCE_SOURCE_ROOT=/build/ethlance
ENV ETHLANCE_ENV=qa
ENV DEPLOY_TARGET=/deploy/

# display contracts
RUN cat shared/src/ethlance/shared/smart_contracts_${ETHLANCE_ENV}.cljs

WORKDIR $ETHLANCE_SOURCE_ROOT/ui
RUN yarn && \
    ETHLANCE_ENV=qa npx shadow-cljs release dev-ui && \
    ./node_modules/less/bin/lessc resources/public/less/main.less resources/public/css/main.css --verbose

FROM nginx:alpine
ENV ETHLANCE_ENV=qa

COPY docker-builds/ui/ethlance.conf /etc/nginx/conf.d/
COPY docker-builds/ui/default.conf /etc/nginx/conf.d/default.conf
COPY docker-builds/ui/nginx.conf /etc/nginx/nginx.conf

COPY --from=build_ui /build/ethlance/resources /deploy/resources
COPY --from=build_ui /build/ethlance/resources/public/contracts /deploy/ui/contracts/
COPY --from=build_ui /build/ethlance/ui/resources/public /deploy/ui/

EXPOSE 80
