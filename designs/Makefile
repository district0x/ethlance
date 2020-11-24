# Makefile for deploying the ethlance design static website as a
# baseline for ethlance ui development
#
# Requires:
#  - php cli (sudo apt-get install php)
#  - gulp (npm install -g gulp)
#
# Quickstart:
#  - make build deploy

.PHONY: help build deploy clean clean-all deps


PHP_SERVER_URL := 0.0.0.0
PHP_SERVER_PORT := 8088


help:
	@echo "Ethlance Static PHP Site"
	@echo ""
	@echo "Development Commands:"
	@echo "  deps               :: Setup initial dependencies."
	@echo "  build              :: Build the src files for deployment."
	@echo "  deploy             :: Run a local PHP server for development."
	@echo ""
	@echo "Misc Commands:"
	@echo "  clean              :: Clean out build artifacts."
	@echo "  help               :: Displays this help message."



build:
	npx gulp build


deploy:
	php --server $(PHP_SERVER_URL):$(PHP_SERVER_PORT) \
      --docroot ./deploy


deps:
	npm install .


clean:
	rm -rf deploy/css
	rm -rf deploy/js
	rm -rf deploy/pages
	rm -rf deploy/css
	rm -rf deploy/index.php


clean-all: clean
	rm -rf node_modules
