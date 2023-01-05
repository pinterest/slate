/*
    Only added for testing: to serve webpack generated production builds in local.
    Just run:  "node server.js"  This will start the express server on the below defined port and serves slate production js, css with proxy
*/
var path = require('path');
var express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const { proxyConfig } = require('./src/setupProxy');

var DIST_DIR = path.join(__dirname, 'build');
var PORT = 3000;
var app = express();

//Serving the files on the dist folder
app.use(express.static(DIST_DIR));

app.use('/api', createProxyMiddleware(proxyConfig));

//Send index.html when the user access the web
app.get('*', function (req, res) {
    res.sendFile(path.join(DIST_DIR, 'index.html'));
});

app.listen(PORT);
