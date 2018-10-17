#!/usr/bin/env bash

# script to setup everything on azure virtual machines

sudo apt-get update
sudo apt-get install memcached git unzip ant openjdk-8-jdk
wget https://github.com/RedisLabs/memtier_benchmark/archive/master.zip
unzip master.zip
cd memtier_benchmark-master
sudo apt-get install build-essential autoconf automake libpcre3-dev libevent-dev pkg-config zlib1g-dev
autoreconf -ivf
./configure
make
sudo service memcached stop
