module.exports = {
  entry: './src/js/deps.js',
  output: {
    path: __dirname + '/resources/public/js',
    filename: 'deps_bundle.js'
  },
  mode: 'development'
};
