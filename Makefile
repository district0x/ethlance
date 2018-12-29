# Makefile for Development and Production Builds

.PHONY: help
.PHONY: dev-server
.PHONY: fig-dev-server fig-dev-ui
.PHONY: build-server build-ui build-contracts build-dist build
.PHONY: watch-contracts watch-tests
.PHONY: testnet ipfs docs
.PHONY: run
.PHONY: deps test travis-test
.PHONY: check clean clean-all

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
	@echo "  watch-contracts         :: Start and watch Solidity Contracts."
	@echo "  watch-tests             :: Start and watch Server tests."
	@echo "  --"
	@echo "  testnet                 :: Start the Testnet server."
	@echo "  ipfs                    :: Start the IPFS daemon."
	@echo "  --"
	@echo "  docs                    :: Generate Documentation"
	@echo ""
	@echo "Production Commands:"
	@echo "  build                   :: Perform Production Build of Ethlance."
	@echo "  --"
	@echo "  build-ui                :: Perform Production Build of Browser UI Only."
	@echo "  build-server            :: Perform Production Build of Node Server Only."
	@echo "  build-contracts         :: Build Solidity Contracts Once."
	@echo "  build-dist              :: Perform Resource Exports into ./dist folder."
	@echo "  --"
	@echo "  run                     :: Run Production Server."
	@echo ""
	@echo "Testing Commands:"
	@echo "  test                    :: Run Server Tests (once)."
	@echo ""
	@echo "Misc Commands:"
	@echo "  deps                    :: Pull and Install third-party dependencies"
	@echo "  check                   :: Checks the status of required pre-requisites"
	@echo "  clean                   :: Clean out build artifacts."
	@echo "  clean-all               :: Clean out more build artifacts."
	@echo "  help                    :: Display this help message."


dev-server:
	node target/node/ethlance_server.js


fig-dev-server:
	lein figwheel dev-server


fig-dev-ui:
	lein figwheel dev-ui


clean:
	lein clean
	rm -rf dist
	rm -f ./resources/ethlance.db


clean-all: clean
	rm -rf node_modules


deps:
	lein deps
	npm install @sentry/node # Hotfix


watch-contracts:
	lein solc auto


watch-tests:
	lein doo node "test-server"


build-ui:
	lein cljsbuild once prod-ui


build-server:
	lein cljsbuild once prod-server


build-contracts:
	lein solc once


build-dist: deps
	cp -R ./resources ./dist/
	cp -R node_modules ./dist/
	sed -i s/ethlance_ui.js/ethlance_ui.min.js/g dist/resources/public/index.html


build: clean-all deps build-ui build-server build-contracts build-dist


test:
	lein doo node "test-server" once


travis-test:
	sh ./scripts/run_test_runner.sh # Produces Correct Error Codes


run:
	cd ./dist && node ./ethlance_server.js


ipfs:
	ipfs daemon


testnet:
	ganache-cli


docs:
	lein marg


check:
	@sh ./scripts/check_prerequisites.sh
