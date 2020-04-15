# Makefile for Development and Production Builds

.PHONY: help
.PHONY: dev-server
.PHONY: fig-dev-all fig-dev-server fig-dev-ui
.PHONY: build-server build-ui build-contracts build-dist build-css build
.PHONY: watch-tests watch-css
.PHONY: deploy testnet ipfs
.PHONY: run build-docs publish-docs
.PHONY: publish-docs-ipfs publish-docs-ipns
.PHONY: deps lein-deps test travis-test
.PHONY: design-build design-deploy design-deps
.PHONY: check clean clean-all


ETHLANCE_ENV := dev # dev, qa, prod
ETHEREUM_NETWORK := ganache # ganache, parity


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
	@echo "  fig-dev-all             :: Start and Watch Server and UI Builds."
	@echo "  fig-dev-server          :: Start and watch Figwheel Server Build."
	@echo "  fig-dev-ui              :: Start and watch Figwheel UI Build."
	@echo "  --"
	@echo "  watch-tests             :: Start and watch Server tests."
	@echo "  watch-css               :: Start and watch CSS Stylesheet Generation (LESS)."
	@echo "  --"
	@echo "  deploy                  :: Deploy Smart Contracts using truffle."
	@echo "  testnet                 :: Start the Testnet server."
	@echo "  ipfs                    :: Start the IPFS daemon."
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


fig-dev-all:
	lein figwheel dev-server dev-ui


fig-dev-server:
	lein figwheel dev-server


fig-dev-ui:
	lein figwheel dev-ui


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
	rm -rf ./node_modules/websocket/.git # Hotfix
	npm install @sentry/node # Hotfix


deps: lein-deps


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
	sh ./scripts/run_test_runner.sh # Produces Correct Error Codes


run:
	cd ./dist && node ./ethlance_server.js


ipfs:
	ipfs daemon


# Environment setup for truffle
TRUFFLE_SCRIPT_FILE := ./node_modules/truffle/build/cli.bundled.js
deploy:
	node $(TRUFFLE_SCRIPT_FILE) migrate --network $(ETHEREUM_NETWORK) --reset


build-contracts:
	node $(TRUFFLE_SCRIPT_FILE) compile


# Environment setup for ganache-cli
TESTNET_SCRIPT_FILE := ./node_modules/ganache-cli/cli.js
TESTNET_PORT := 8549
testnet:
	node $(TESTNET_SCRIPT_FILE) -m district0x -p $(TESTNET_PORT) $(TESTNET_OPTIONS) -l 8000000


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
LESS_WATCH_SCRIPT := ./node_modules/less-watch-compiler/dist/less-watch-compiler.js
LESS_BIN_PATH := ./node_modules/less/bin
PATH := $(LESS_BIN_PATH):$(PATH)


watch-css:
	node $(LESS_WATCH_SCRIPT) resources/public/less resources/public/css main.less


build-css:
	$(LESS_BIN_PATH)/lessc resources/public/less/main.less resources/public/css/main.css


design-deps:
	make -C ./designs deps


design-build:
	make -C ./designs build


design-deploy:
	make -C ./designs deploy
