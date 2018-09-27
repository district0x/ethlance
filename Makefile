# Makefile for Development and Production Builds

.PHONY: help open-dev-server

help:
	@echo "Ethlance Development and Production Build Makefile"
	@echo ""
	@echo "Commands:"
	@echo "  open-dev-server         Open Development Node Server for figwheel."


open-dev-server:
	node target/node/ethlance_server.js
