postgres: make postgres
ipfs: make ipfs
testnet: make testnet
deploy_contracts: sleep 5 && make deploy && sleep infinity
css: make watch-css
ui: cd ui && yarn watch
server: sleep 10 && cd server && yarn watch         # Wait for testnet to go up
graphql: sleep 10 && node server/ethlance_server.js # Wait for DB to go up
designs: make design
