const path = require('path');
const HtmlWebPackPlugin = require('html-webpack-plugin');
const ESLintPlugin = require('eslint-webpack-plugin');
const { proxyConfig } = require('./src/setupProxy');

const USER = process.env.USER;
const GROUP = process.env.GROUP;
console.log(`Username is: ${USER} & Group is: ${GROUP}`);

module.exports = (env) => {
    return {
        mode: env.prod ? 'production' : 'development',
        devtool: env.prod ? 'source-map' : 'eval-cheap-module-source-map',
        entry: './src/index.js',
        output: {
            path: path.resolve(__dirname, 'build'),
            publicPath: '/',
            filename: env.prod ? 'static/js/[name].[contenthash:8].js' : 'static/js/bundle.js',
        },
        resolve: {
            modules: [__dirname, 'node_modules'],
            extensions: ['.js', '.jsx', '.json', '.ts', '.tsx', '.scss'],
        },
        devServer: {
            client: {
                // disable warning popup for eslint warnings
                overlay: {
                    warnings: false,
                    errors: true,
                },
            },
            hot: true,
            historyApiFallback: true, // for react route to work
            proxy: {
                '/api': proxyConfig,
            },
            port: 10001,
        },
        module: {
            rules: [
                {
                    test: /\.(js|jsx)?$/,
                    exclude: [path.resolve(__dirname, 'node_modules')],
                    use: {
                        loader: 'babel-loader',
                    },
                },
                // babel can used for typescript too, but standard & better option is ts-loader
                { test: /\.tsx?$/, use: 'ts-loader' },
                {
                    test: /\.(scss|css)$/,
                    use: [
                        'style-loader', // creates style nodes from JS strings
                        'css-loader', // translates CSS into CommonJS
                        'sass-loader', // compiles Sass to CSS, using Node Sass by default
                    ],
                },
                {
                    test: /.(woff)(.*)?/,
                    use: {
                        loader: 'url-loader',
                    },
                },
            ],
        },
        plugins: [
            new HtmlWebPackPlugin({
                template: './public/index.html',
                filename: './index.html',
            }),
            new ESLintPlugin(),
        ],
    };
};
