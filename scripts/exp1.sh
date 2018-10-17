#!/usr/bin/env bash
# run the middleware for one hour, observe if there are any abnormal phenomenons

# usage: ./exp1.sh [test time] [repetitions] [nopop]

if [ -z "${VM_NAME}" ]; then
    echo "Variable VM_NAME not set!"
    exit
fi

logfile="exp1_${VM_NAME}.log"

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
    time="3600"
else
    time=$1
fi

if [ -z $2 ]; then
    repetitions="1"
else
    repetitions=$2
fi

cmdpart="memtier_benchmark --port=11211 --protocol=memcache_text --expiry-range=9999-10000 --key-maximum=10000 --data-size=4096"
fnamepart="../logs/1"
clients=(1 2 4 8 16 32) # (1 2 4 8 12 16 20 24 32)

# pre-populate the memcached servers
echolog "# ASL section 1 experiments"
echolog "Run each experiment with ${time} seconds and ${repetitions} repetitions"
if [[ $3 != "nopop" ]]; then
    echolog "Populating server: server1"
    ./prepopulate.sh 30 server1
    echolog
fi

[ -e ${fnamepart} ] && backup="../logs/backup1_$(date +%Y-%m-%d_%H-%M-%S)" && echolog "Old data folder found, renaming to ${backup}" && mv ${fnamepart} ${backup}
echolog
mkdir -p ${fnamepart}

if [[ ${type} == "memtier" ]]; then
    # run ping once before experiments
    cmd="ping middleware1 > ${fnamepart}/ping_${VM_NAME}_to_middleware1.log & "
    echolog ${cmd}
    eval ${cmd}
    pidping=$!
    sleep 30
    echolog "killing ping"
    kill -2 ${pidping}

    for r in `seq 1 ${repetitions}`; do
        echolog "================================================================="
        echolog "Test with CT = 2, VC = 32, repetition $r begin"
        echolog "-----------------------------------------------------------------"
        echolog "Waiting for 10 secs for middleware to start..."
        sleep 10  # give middleware some time to start
        echolog "Run test (of ${time} secs) now"
        dir="${fnamepart}"
        mkdir -p ${dir}
        # run dstat
        cmd="dstat -tcnm --output ${dir}/dstat_${VM_NAME}.csv > ${dir}/dstat_${VM_NAME}.log & "
        echolog ${cmd}
        eval ${cmd}
        piddstat=$!
        # run memtier
        cmd="${cmdpart} --ratio=1:1 --server=middleware1 --test-time=${time} --clients=32 --threads=2 > ${dir}/memtier_${VM_NAME}0.log 2>&1 &"
        echolog ${cmd}
        eval ${cmd}
        pidmemtier=$!
        echolog "Waiting for $((time+10)) secs for test to fully stop..."
        sleep $((time+10))
        echolog "Kill memtier and dstat (if exist)"
        kill ${pidmemtier}  # if memtier didn't stop, force kill it
        kill ${piddstat}
        echolog "Waiting for extra 10 secs for middleware to fully stop..."
        sleep 10  # give middleware to stop (dump statistics)
        echolog "-----------------------------------------------------------------"
        echolog "Test with CT = 2, VC = 32, repetition $r end"
        echolog "================================================================="
        echolog
        echolog
    done
else
    # middleware side
    # run ping once before experiments
    cmd="ping server1 > ${fnamepart}/ping_${VM_NAME}_to_server1.log & "
    echolog ${cmd}
    eval ${cmd}
    pidping=$!
    sleep 30
    echolog "killing ping"
    kill -2 ${pidping}

    cmdpart="java -Dlog4j.configurationFile=../middleware/lib/log4j2.xml -cp ../middleware/dist/middleware-zhiyang.jar:../middleware/lib/* ch.ethz.asltest.RunMW -l 0.0.0.0 -p 11211 -s false -m server1:11211"

    [ -e ${fnamepart} ] && backup="../logs/backup1_$(date +%Y-%m-%d_%H-%M-%S)" && echolog "Old data folder found, renaming to ${backup}" && mv ${fnamepart} ${backup}
    echolog
    for r in `seq 1 ${repetitions}`; do
        echolog "================================================================="
        echolog "Test with CT = 2, VC = 32, MW workers = 32, repetition $r begin"
        echolog "-----------------------------------------------------------------"
        dir="${fnamepart}"
        mkdir -p ${dir}
        # run dstat
        cmd="dstat -tcnm --output ${dir}/dstat_${VM_NAME}.csv > ${dir}/dstat_${VM_NAME}.log & "
        echolog ${cmd}
        eval ${cmd}
        piddstat=$!
        # run middleware
        cmd="${cmdpart} -t 32 > ${dir}/mw_00.log & "
        echolog ${cmd}
        eval ${cmd}
        pid=$!
        echolog "Sleeping $((time+20)) secs for the test to finish..."
        # kill all
        sleep $((time+20))
        echolog "Kill java, ping, dstat (if exist)"
        kill ${pid}
        kill ${piddstat}
        echolog "Sleeping 10 extra secs for logs to be fully dumped..."
        sleep 10
        echolog "Kill java again if exists"
        kill -9 ${pid}  # in case middleware mysteriously cannot stop
        echolog "Test with CT = 2, VC = 32, MW workers = 32, repetition $r end"
        echolog "================================================================="
        echolog; echolog
    done
fi

echolog "# ASL section 1 experiments ALL DONE."