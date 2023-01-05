const USER = process.env.USER;
const GROUP = process.env.GROUP;

function onProxyReq(proxyReq, req, res) {
    // add custom header to request
    proxyReq.setHeader('x-forwarded-user', USER);
    proxyReq.setHeader('x-forwarded-groups', GROUP);
    // or log the req
}

const proxyConfig = {
    target: 'http://localhost:8090',
    changeOrigin: true,
    ws: true,
    onProxyReq: onProxyReq,
};

module.exports = { proxyConfig };
