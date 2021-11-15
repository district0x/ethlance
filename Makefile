# Makefile for Development and Production Builds

.PHONY: help
.PHONY: dev-server
.PHONY: fig-dev-all fig-dev-server fig-dev-ui
.PHONY: build-server build-ui build-contracts build-dist build-css build
.PHONY: watch-tests watch-css
.PHONY: deploy testnet ipfs postgres
.PHONY: run build-docs publish-docs
.PHONY: publish-docs-ipfs publish-docs-ipns
.PHONY: deps lein-deps test travis-test
.PHONY: design-build design-deploy design-deps
.PHONY: check clean clean-all


ETHLANCE_ENV := dev # dev, qa, prod
ETHEREUM_NETWORK := ganache # hostia, ganache, parity


#
# Start
#

help:
	@echo "Ethlance Development and Production Build Makefile"
	@echo ""
	@echo "Development Commands:"
	@echo "  dev-server              :: Run Development Node Server for Figwheel Server Build"
	@echo "  repl                    :: Start CLJ Repl."
	@echo "  --"
	@echo "  fig-dev-server          :: Start and watch Figwheel Server Build."
	@echo "  fig-dev-ui              :: Start and watch Figwheel UI Build."
	@echo "  --"
	@echo "  watch-tests             :: Start and watch Server tests."
	@echo "  watch-css               :: Start and watch CSS Stylesheet Generation (LESS)."
	@echo "  --"
	@echo "  deploy                  :: Deploy Smart Contracts using truffle."
	@echo "  testnet                 :: Start the Testnet server."
	@echo "  ipfs                    :: Start the IPFS daemon."
	@echo "  postgres                :: Start the postgresql database as a docker container 'dev-ethlance-psql'"
	@echo "  --"
	@echo "  build-docs              :: Generate Requirement, Design, and Spec Documents"
	@echo "  publish-docs            :: Publish the documentation to IPFS"
	@echo "  publish-docs-ipfs       :: Publish the documentation to IPFS"
	@echo "  publish-docs-ipns       :: Publish the documentation to IPNS"
	@echo ""
	@echo "Production Commands:"
	@echo "  build                   :: Perform Production Build of Ethlance."
	@echo "  --"
	@echo "  build-ui                :: Perform Production Build of Browser UI Only."
	@echo "  build-server            :: Perform Production Build of Node Server Only."
	@echo "  build-contracts         :: Build Solidity Contracts Once."
	@echo "  build-css               :: Build CSS Stylesheets Once."
	@echo "  build-dist              :: Perform Resource Exports into ./dist folder."
	@echo "  --"
	@echo "  run                     :: Run Production Server."
	@echo ""
	@echo "Testing Commands:"
	@echo "  test                    :: Run Server Tests (once)."
	@echo ""
	@echo "Design Commands:"
	@echo "  design-deps             :: Initial Setup of the Design Site. (once)"
	@echo "  design-build            :: Build Ethlance Design Static Website."
	@echo "  design-deploy           :: Deploy Ethlance Design Static Website."
	@echo ""
	@echo "Misc Commands:"
	@echo "  deps                    :: Pull and Install third-party dependencies."
	@echo "  check                   :: Checks the status of required pre-requisites."
	@echo "  clean                   :: Clean out build artifacts."
	@echo "  clean-all               :: Clean out more build artifacts."
	@echo "  help                    :: Display this help message."


dev-server:
	node target/node/ethlance_server.js


fig-dev-server:
	lein with-profile +dev-server figwheel dev-server

watch-server:
	npx shadow-cljs watch dev-server

watch-ui:
	npx shadow-cljs watch dev-ui

clean:
	lein clean
	rm -rf dist
	rm -f ./resources/ethlance.db
	rm -rf ./resources/public/css
	rm -rf ./resources/public/contracts/build

clean-all: clean
	rm -rf node_modules
	make -C ./designs clean
	make -C ./docs clean

lein-deps:
	rm -rf ./node_modules/websocket/.git # Hotfix
	lein deps


npm-deps:
	  yarn deps
	# rm -rf ./node_modules/websocket/.git # Hotfix
	# lein npm install
	# npm install @sentry/node # Hotfix


deps: lein-deps npm-deps


watch-tests:
	lein doo node "test-server"


build-ui:
	lein cljsbuild once prod-ui


build-server:
	lein cljsbuild once prod-server


build-dist:
	cp -R ./resources ./dist/
	cp -R node_modules ./dist/
	sed -i s/ethlance_ui.js/ethlance_ui.min.js/g dist/resources/public/index.html


build: build-ui build-server build-contracts build-css build-dist


test:
	lein doo node "test-server" once


travis-test:
	sh ./scripts/run_test_runner.sh # Produces Correct Exit Codes


run:
	cd ./dist && node ./ethlance_server.js


ipfs:
	ipfs daemon --api "/ip4/0.0.0.0/tcp/5001"


ETHLANCE_DB_PORT := 5432
postgres:
	docker run                                                       \
		   --name    dev-ethlance-psql                               \
		   --volume  dev-ethlance-psql-data:/var/lib/postgresql/data \
		   --publish $(ETHLANCE_DB_PORT):5432                        \
		   --env     POSTGRES_DB=ethlance                            \
		   --env     POSTGRES_USER=user                              \
		   --env     POSTGRES_PASSWORD=pass                          \
		   --rm                                                      \
		   postgres:11


deploy:
	npx truffle migrate --network $(ETHEREUM_NETWORK) --reset


build-contracts:
	npx truffle compile


# Environment setup for ganache-cli
TESTNET_PORT := 8549
TESTNET_HOST := 0.0.0.0
testnet:
	npx ganache-cli --mnemonic "easy leave proof verb wait patient fringe laptop intact opera slab shine" --host $(TESTNET_HOST) --port $(TESTNET_PORT) $(TESTNET_OPTIONS) -l 8000000 --allowUnlimitedContractSize
	# npx ganache-cli --mnemonic $(TESTNET_SEED_PHRASE) --host $(TESTNET_HOST) --port $(TESTNET_PORT) $(TESTNET_OPTIONS) -l 8000000 --allowUnlimitedContractSize
	# npx ganache-cli --mnemonic district0x --host $(TESTNET_HOST) --port $(TESTNET_PORT) $(TESTNET_OPTIONS) -l 8000000


build-docs:
	make -C ./docs


publish-docs: publish-docs-ipfs


publish-docs-ipfs:
	make -C ./docs publish-ipfs


publish-docs-ipns:
	make -C ./docs publish-ipns


check:
	@sh ./scripts/check_prerequisites.sh


# Environment Setup for lessc, and less-watch-compiler
LESS_BIN_PATH := ./ui/node_modules/less/bin
PATH := $(LESS_BIN_PATH):$(PATH)
build-css:
	$(LESS_BIN_PATH)/lessc resources/public/less/main.less resources/public/css/main.css


watch-css: build-css
	npx less-watch-compiler resources/public/less resources/public/css main.less


design-deps:
	make -C ./designs deps


design-build:
	make -C ./designs build


design-deploy:
	make -C ./designs deploy

design: design-deps design-build design-deploy
