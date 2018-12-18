#!/usr/bin/env bash
# test the limit of one load generating (memtier) machine using 2 servers
# usage: ./exp2.2.sh [test time] [repetitions] [nopop]

if [ -z "${VM_NAME}" ]; then
    echo "Variable VM_NAME not set!"
    exit
fi

logfile="exp2.2_${VM_NAME}.log"

echo "Logging running info to ${logfile}"
[ -e ${logfile} ] && rm ${logfile}

echolog() {
    echo `date +%Y-%m-%d\ %H:%M:%S` $@ | tee -a ${logfile}
}


if [[ ${VM_NAME} == server* ]]; then
    echolog "This is a memcached machine. This script is not for it."
    exit
fi

if [[ ${VM_NAME} == mw* ]]; then
    type="middleware"
else
    type="memtier"
fi
echolog "Running on ${VM_NAME}. This is a ${type} machine."
echolog


if [ -z $1 ]; then
    time="80"
else
    time=$1
fi

if [ -z $2 ]; then
    repetitions="3"
else
    repetitions=$2
fi

cmdpart="memtier_benchmark --port=11211 --protocol=memcache_text --expiry-range=99999-100000 --key-maximum=10000 --data-size=4096"
fnamepart="../logs/2.2"
clients=(1 2 3 4 5 6 8 16 32)

# pre-populate the memcached servers
echolog "# ASL section 2.2 experiments"
echolog "Run each experiment with ${time} seconds and ${repetitions} repetitions"
if [[ $3 != "nopop" ]]; then
    echolog "Populating server: server1 server2"
    ./prepopulate.sh 30 server1 server2
    echolog
fi


# run ping once before experiments
cmd1="ping server1 > ${fnamepart}/ping_${VM_NAME}_to_server1.log & "
echolog ${cmd1}
eval ${cmd1}
pidping1=$!
cmd2="ping server2 > ${fnamepart}/ping_${VM_NAME}_to_server2.log & "
echolog ${cmd2}
eval ${cmd2}
pidping2=$!
sleep 30
echolog "kill all pings"
kill -2 ${pidping1}
kill -2 ${pidping2}

# readonly workloads
echolog "# Readonly workloads"
for c in "${clients[@]}"; do
    for r in `seq 1 ${repetitions}`; do
        echolog "================================================================="
        echolog "Test with CT = 1, VC = $c, repetition $r begin"
        echolog "-----------------------------------------------------------------"
        dir="${fnamepart}/readonly/vc${c}/r${r}"
        mkdir -p ${dir}
        # run dstat
        cmd="dstat -tcnm --output ${dir}/dstat_${VM_NAME}.csv > ${dir}/dstat_${VM_NAME}.log & "
        echolog ${cmd}
        eval ${cmd}
        piddstat=$!
        # run memtier
        cmd1="${cmdpart} --ratio=0:1 --test-time=${time} --clients=${c} --threads=1 --server=server1 > ${dir}/${VM_NAME}0.log 2>&1 & "
        cmd2="${cmdpart} --ratio=0:1 --test-time=${time} --clients=${c} --threads=1 --server=server2 > ${dir}/${VM_NAME}1.log 2>&1 & "
        echolog ${cmd1}
        eval ${cmd1}
        pid1=$!
        echolog ${cmd2}
        eval ${cmd2}
        pid2=$!
        echolog "Waiting for $((time+10)) secs for test to fully stop..."
        sleep $((time+10))
        echolog "Kill memtier, ping and dstat (if exist)"
        kill ${pid1}
        kill ${pid2}
        kill ${piddstat}
        echolog "-----------------------------------------------------------------"
        echolog "Test with CT = 1, VC = $c, repetition $r end"
        echolog "================================================================="
        echolog
        echolog
    done
done

