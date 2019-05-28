# Makefile for deploying the ethlance design static website as a
# baseline for ethlance ui development
#
# Requires:
#  - php cli (sudo apt-get install php)
#  - gulp (npm install -g gulp)
#
# Quickstart:
#  - make build deploy

.PHONY: help build deploy clean


SERVER_PORT := 8080


help:
	@echo "Ethlance Static PHP Site"
	@echo ""
	@echo "Development Commands:"
	@echo "  build              :: Build the src files for deployment."
	@echo "  deploy             :: Run a local PHP server for development."
	@echo ""
	@echo "Misc Commands:"
	@echo "  clean              :: Clean out build artifacts."
	@echo "  help               :: Displays this help message."


build:
	gulp build


deploy:
	cd ./deploy && php -S localhost:$(SERVER_PORT)


clean:
	rm -rf deploy
