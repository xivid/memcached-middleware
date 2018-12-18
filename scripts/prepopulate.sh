#!/usr/bin/env bash
# Populates the memcached server. Uses "Sequential" key pattern so that all keys can be filled at least once, while spending very little time.

if [[ $# < 2 ]]; then
	echo "Usage: ./prepopulate.sh [running time] [servers ...]"
	exit
fi

time=$1

servers=()
for i in "${@:2}"; do
	servers=("${servers[@]}" "$i")
done

echo "Filling servers ${servers[@]}."
for server in "${servers[@]}"; do
    #add parameters to the command
    cmd="memtier_benchmark --port=11211 --protocol=memcache_text --clients=1 --threads=1 --ratio=1:0 --key-pattern=S:S --expiry-range=99999-100000 --key-maximum=10000 --data-size=4096 --hide-histogram --server=${server} --test-time=${time}"
    #run the command
    echo $cmd
    $cmd > /dev/null 2>&1 &
done
wait `jobs -p`
echo "Filled servers ${servers[@]}."