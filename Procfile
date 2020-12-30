postgres: make postgres
ipfs: make ipfs
testnet: make testnet
deploy_contracts: sleep 5 && make deploy && sleep infinity # Wait a bit for testnet to start
css: make watch-css
shadow: npx shadow-cljs server
ui_js: ./bin/repl-run "(shadow/watch :dev-ui) @(promise)" # Avoid repl exiting to keep STDOUT coming
server_js: ./bin/repl-run "(shadow/watch :dev-server) @(promise)" # Avoid repl exiting to keep STDOUT coming
server: ./bin/retry-run-node-server # Start after postgres & testnet are up
designs: make design # port: 8088
