# To run these you can use https://github.com/DarthSim/overmind
testnet: bb testnet
# deploy_contracts: sleep 5 && npx truffle migrate --network ganache --reset && sleep infinity # Wait a bit for testnet to start
css: bb watch-css
ui_js: bb watch-ui \:local-deps
server_js: bb watch-server \:local-deps
server: cd server && node out/ethlance_server.js
designs: bb serve-designs
