ETHLANCE_SOURCE_ROOT=/home/madis/code/district0x/ethlance
ETHLANCE_ENV=qa
DEPLOY_TARGET=mad.is:~/www/ethlance

## Smart contracts
cd $ETHLANCE_SOURCE_ROOT
ETHLANCE_ENV=qa npx truffle migrate --network ethlance.mad.is-testnet --reset
#
#####################
## UI: prepare assets
cd $ETHLANCE_SOURCE_ROOT/ui && yarn
rm -rf .cpcache/* .shadow-cljs/*
cd $ETHLANCE_SOURCE_ROOT/ui && ETHLANCE_ENV=qa npx shadow-cljs -A:local-deps release dev-ui
cd $ETHLANCE_SOURCE_ROOT/ui && ./node_modules/less/bin/lessc resources/public/less/main.less resources/public/css/main.css --verbose

## UI: copy assets
rsync --progress -ru resources/public/{css,images,index.html,js} ../resources/public/contracts $DEPLOY_TARGET/ui

##################
## Server: compile
ETHLANCE_SERVER_ROOT=/home/madis/temp/ethlance/server

cd $ETHLANCE_SOURCE_ROOT/server
# Need to clear shadow-cljs cache to force the slurp macro to be evaluated again
# because the clojure file didn't change but the .edn config file did
# Alternatively the ethlance.server.core must be touched after changing config
rm -rf .shadow-cljs/* .cpcache/*

# Release deployment
ETHLANCE_ENV=qa npx shadow-cljs -A:local-deps release dev-server
rsync --progress -ru out/ethlance_server.js out/ethlance_server.js.map package.json $DEPLOY_TARGET/server

## Start server (on remote machine/container)
cd $ETHLANCE_SERVER_ROOT && yarn
cd $ETHLANCE_SERVER_ROOT && node ethlance_server.js

## Server Postgres
# $ sudo -u postgres psql
# > CREATE DATABASE ethlance;
# > CREATE USER ethlanceuser WITH ENCRYPTED PASSWORD 'pass';
# > GRANT ALL PRIVILEGES ON DATABASE ethlance TO ethlanceuser;
