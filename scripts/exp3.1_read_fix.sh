#!/usr/bin/env bash
# one middleware experiment: 3 clients, 1 server
# usage: ./exp3.1_read.sh [time] [repetitions] [nopop]

if [ -z "${VM_NAME}" ]; then
    echolog "Variable VM_NAME not set!"
    exit
fi

logfile="exp3.1_read_${VM_NAME}.log"

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

fnamepart="../logs/3.1"
workloads=("readonly")
clients=(32)
workers=(32 64)

echolog "# ASL section 3.1 readonly experiments"
echolog "Run each experiment with ${time} seconds and ${repetitions} repetitions"

# pre-populate the memcached servers
if [[ $3 != "nopop" ]]; then
    echolog "Populating server: server1"
    ./prepopulate.sh 30 server1
    echolog
fi

if [[ ${type} == "memtier" ]]; then
    # client side
    cmdpart="memtier_benchmark --port=11211 --protocol=memcache_text --expiry-range=99999-100000 --key-maximum=10000 --data-size=4096"

    for workload in "${workloads[@]}"; do
        if [[ ${workload} == "readonly" ]]; then
            ratio="0:1"
        else
            ratio="1:0"
        fi
        for c in "${clients[@]}"; do
            for w in "${workers[@]}"; do
                for r in `seq 1 ${repetitions}`; do
                    echolog "==========================================================================="
                    echolog "${workload} test with CT = 2, VC = $c, MW workers = $w, repetition $r begin"
                    echolog "---------------------------------------------------------------------------"
                    echolog "Waiting for 10 secs for middleware to start..."
                    sleep 10  # give middleware some time to start
                    echolog "Run test (of ${time} secs) now"
                    dir="${fnamepart}/${workload}/vc${c}/w${w}/r${r}"
                    mkdir -p ${dir}
                    # run ping
                    cmd="ping middleware1 > ${dir}/ping_${VM_NAME}_to_middleware1.log & "
                    echolog ${cmd}
                    eval ${cmd}
                    pidping=$!
                    # run dstat
                    cmd="dstat -tcnm --output ${dir}/dstat_${VM_NAME}.csv > ${dir}/dstat_${VM_NAME}.log & "
                    echolog ${cmd}
                    eval ${cmd}
                    piddstat=$!
                    # run memtier
                    cmd="${cmdpart} --ratio=${ratio} --server=middleware1 --test-time=${time} --clients=${c} --threads=2 > ${dir}/${VM_NAME}0.log 2>&1 &"
                    echolog ${cmd}
                    eval ${cmd}
                    pid=$!
                    echolog "Waiting for $((time+10)) secs for test to fully stop..."
                    sleep $((time+10))
                    echolog "Kill memtier, ping and dstat (if exist)"
                    kill ${pid}
                    kill -2 ${pidping}
                    kill ${piddstat}
                    echolog "Waiting for extra 10 secs for middleware to fully stop..."
                    sleep 10  # give middleware to stop (dump statistics)
                    echolog "---------------------------------------------------------------------------"
                    echolog "${workload} test with CT = 2, VC = $c, MW workers = $w, repetition $r end"
                    echolog "==========================================================================="
                    echolog; echolog
                done
            done
        done
    done
else
    # middleware side
    cmdpart="java -Dlog4j.configurationFile=../middleware/lib/log4j2.xml -cp ../middleware/dist/middleware-zhiyang.jar:../middleware/lib/* ch.ethz.asltest.RunMW -l 0.0.0.0 -p 11211 -s false -m server1:11211"

    for workload in "${workloads[@]}"; do
        for c in "${clients[@]}"; do
            for w in "${workers[@]}"; do
                for r in `seq 1 ${repetitions}`; do
                    echolog "==========================================================================="
                    echolog "${workload} test with CT = 2, VC = $c, MW workers = $w, repetition $r begin"
                    echolog "---------------------------------------------------------------------------"
                    dir="${fnamepart}/${workload}/vc${c}/w${w}/r${r}"
                    mkdir -p ${dir}
                    # run ping
                    cmd="ping server1 > ${dir}/ping_${VM_NAME}.log & "
                    echolog ${cmd}
                    eval ${cmd}
                    pidping=$!
                    # run dstat
                    cmd="dstat -tcnm --output ${dir}/dstat_${VM_NAME}.csv > ${dir}/dstat_${VM_NAME}.log & "
                    echolog ${cmd}
                    eval ${cmd}
                    piddstat=$!
                    # run middleware
                    cmd="${cmdpart} -t ${w} > ${dir}/${VM_NAME}.log & "
                    echolog ${cmd}
                    eval ${cmd}
                    pid=$!
                    echolog "Sleeping $((time+20)) secs for the test to finish..."
                    # kill all
                    sleep $((time+20))
                    echolog "Kill java, ping, dstat (if exist)"
                    kill ${pid}
                    kill -2 ${pidping}
                    kill ${piddstat}
                    echolog "Sleeping 10 extra secs for logs to be fully dumped..."
                    sleep 10
                    echolog "Kill java again if exists"
                    kill -9 ${pid}  # in case middleware mysteriously cannot stop
                    echolog "---------------------------------------------------------------------------"
                    echolog "${workload} test with CT = 2, VC = $c, MW workers = $w, repetition $r end"
                    echolog "==========================================================================="
                    echolog; echolog
                done
            done
        done
    done
fi

echolog "# ASL section 3.1 readonly experiments ALL DONE."