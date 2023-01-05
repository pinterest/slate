#!/bin/bash


CWD="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"

NODE_DIR=$CWD/../src/main/resources/webapp

cd $NODE_DIR
npm install --legacy-peer-deps -f
npm run build