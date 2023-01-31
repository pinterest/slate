#!/bin/bash


CWD="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"

NODE_DIR=$CWD/../src/main/resources/webapp

cd $NODE_DIR
npm install -g npm@7
npm install --legacy-peer-deps -f
npm run build