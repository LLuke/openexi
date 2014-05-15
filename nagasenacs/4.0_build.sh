#!/bin/sh
nant -t:net-4.0 clean
nant -t:net-4.0 -D:debug=true compile
