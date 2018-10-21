#!/usr/bin/env bash
# test the bandwidth between clients and middlewares/servers

# usage: ./iperf.sh [servers...]

if [ -z "${VM_NAME}" ]; then
    echo "Variable VM_NAME not set!"
    exit
fi

if [[ $# < 1 ]]; then
	echo "Usage: ./prepopulate.sh [running time] [servers ...]"
	exit
fi

servers=()
for i in "${@:1}"; do
	servers=("${servers[@]}" "$i")
done

echo "Testing iperf with servers ${servers[@]}"
for server in "${servers[@]}"; do
    echo "Testing with ${server}"
    for i in `seq 1 3`; do
        iperf -c ${server} >> iperf_${VM_NAME}_to_${server}.log
    done
done
