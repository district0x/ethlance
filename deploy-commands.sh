export ETHLANCE_SOURCE_ROOT=/home/ubuntu/proj/0x/git/ethlance
export ETHLANCE_ENV=prod
export DEPLOY_TARGET=/home/ubuntu/www/ethlance

## Smart contracts
cd $ETHLANCE_SOURCE_ROOT
ETHLANCE_ENV=prod npx truffle migrate --network ethlance.mad.is-testnet --reset

#####################
## UI: prepare assets
#####################
cd $ETHLANCE_SOURCE_ROOT/ui && yarn
rm -rf .cpcache/* .shadow-cljs/*
export ETHLANCE_CONFIG_PATH=/home/ubuntu/proj/0x/git/ethlance/ethlance-config/config/ui-config-prod.edn
export SMART_CONTRACTS_BUILD_PATH=${ETHLANCE_SOURCE_ROOT}/resources/public/contracts/build
export SMART_CONTRACTS=${ETHLANCE_SOURCE_ROOT}/shared/src/ethlance/shared/smart_contracts_prod.cljs
cd $ETHLANCE_SOURCE_ROOT/ui && ETHLANCE_ENV=prod npx shadow-cljs release dev-ui
# To compile with local dependencies defined under :local-deps alias
ETHLANCE_ENV=prod clj -A:dev:shadow-cljs:local-deps release dev-ui

cd $ETHLANCE_SOURCE_ROOT/ui && ./node_modules/less/bin/lessc resources/public/less/main.less resources/public/css/main.css --verbose

## UI: copy assets
rsync --progress -ru resources/public/{css,images,index.html,js} ../resources/public/contracts $DEPLOY_TARGET/ui

##################
## Server: compile
##################
ETHLANCE_SERVER_ROOT=${DEPLOY_TARGET}/server

cd $ETHLANCE_SOURCE_ROOT/server
# Need to clear shadow-cljs cache to force the slurp macro to be evaluated again
# because the clojure file didn't change but the .edn config file did
# Alternatively the ethlance.server.core must be touched after changing config
rm -rf .shadow-cljs/* .cpcache/* out/*

# Release deployment
export ETHLANCE_CONFIG_PATH=${ETHLANCE_SOURCE_ROOT}-config/config/server-config-prod.edn
ETHLANCE_ENV=prod npx shadow-cljs release dev-server
# To compile including local changes to  libraries:
ETHLANCE_ENV=prod clj -A:dev:shadow-cljs:local-deps release dev-server
rsync --progress -ru out/ethlance_server.js out/ethlance_server.js.map package.json $DEPLOY_TARGET/server

## Start server (on remote machine/container)
cd $ETHLANCE_SERVER_ROOT && yarn
cd $ETHLANCE_SERVER_ROOT && node ethlance_server.js

## Server Postgres
# sudo -u postgres psql <<EOS
# CREATE DATABASE ethlance;
# CREATE USER ethlanceuser WITH ENCRYPTED PASSWORD 'pass';
# GRANT ALL PRIVILEGES ON DATABASE ethlance TO ethlanceuser;
# EOS
