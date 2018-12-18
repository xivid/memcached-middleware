#!/usr/bin/env bash
# two middlewares experiment: 3 clients, 3 server
# usage: ./exp5.sh [time] [repetitions] [nopop]

[ -e exp5_${VM_NAME}.log ] && rm exp5_${VM_NAME}.log

echolog() {
    echo `date +%Y-%m-%d\ %H:%M:%S` $@ | tee -a exp5_${VM_NAME}.log
}

if [ -z "${VM_NAME}" ]; then
    echolog "Variable VM_NAME not set!"
    exit
fi

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

fnamepart="../logs/5.1"
keys=(1 3 6 9)

echolog "# ASL section 5 experiments: 5.1 sharded multigets"
echolog "Run each experiment with ${time} seconds and ${repetitions} repetitions"

# pre-populate the memcached servers
if [[ $3 != "nopop" ]]; then
    echolog "Populating server: server1 server2 server3"
    ./prepopulate.sh 30 server1 server2 server3
    echolog
fi

# 5.1 Sharded
if [[ ${type} == "memtier" ]]; then
    # client side
    cmdpart="memtier_benchmark --port=11211 --protocol=memcache_text --expiry-range=99999-100000 --key-maximum=10000 --data-size=4096 --clients=2 --threads=1"
    echolog

    for key in "${keys[@]}"; do
        for r in `seq 1 ${repetitions}`; do
            echolog "============================================================================================"
            echolog "Sharded test with multi-key-get=${key}, repetition $r begin"
            echolog "--------------------------------------------------------------------------------------------"
            echolog "Waiting for 10 secs for middleware to start..."
            sleep 10  # give middleware some time to start
            echolog "Run test (of ${time} secs) now"
            dir="${fnamepart}/k${key}/r${r}"
            mkdir -p ${dir}
            # run ping
            cmd1="ping middleware1 > ${dir}/ping_${VM_NAME}_to_middleware1.log & "
            echolog ${cmd1}
            eval ${cmd1}
            pidping1=$!
            cmd2="ping middleware2 > ${dir}/ping_${VM_NAME}_to_middleware2.log & "
            echolog ${cmd2}
            eval ${cmd2}
            pidping2=$!
            # run dstat
            cmd="dstat -tcnm --output ${dir}/dstat_${VM_NAME}.csv > ${dir}/dstat_${VM_NAME}.log & "
            echolog ${cmd}
            eval ${cmd}
            piddstat=$!
            # run memtier
            cmd1="${cmdpart} --multi-key-get=${key} --ratio=1:${key} --server=middleware1 --test-time=${time} > ${dir}/${VM_NAME}0.log 2>&1 &"
            cmd2="${cmdpart} --multi-key-get=${key} --ratio=1:${key} --server=middleware2 --test-time=${time} > ${dir}/${VM_NAME}1.log 2>&1 &"
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
            kill -2 ${pidping1}
            kill -2 ${pidping2}
            kill ${piddstat}
            echolog "Waiting for extra 10 secs for middleware to fully stop..."
            sleep 10  # give middleware to stop (dump statistics)
            echolog "------------------------------------------------------------------------------------------"
            echolog "Sharded test with multi-key-get=${key}, repetition $r end"
            echolog "=========================================================================================="
            echolog; echolog
        done
    done
else
    # middleware side
    cmdpart="java -Dlog4j.configurationFile=../middleware/lib/log4j2.xml -cp ../middleware/dist/middleware-zhiyang.jar:../middleware/lib/* ch.ethz.asltest.RunMW -l 0.0.0.0 -p 11211 -t 64 -s true -m server1:11211 server2:11211 server3:11211"

    echolog

    for key in "${keys[@]}"; do
        for r in `seq 1 ${repetitions}`; do
            echolog "============================================================================================"
            echolog "Sharded test with multi-key-get=${key}, repetition $r begin"
            echolog "--------------------------------------------------------------------------------------------"
            dir="${fnamepart}/k${key}/r${r}"
            mkdir -p ${dir}
            # run ping
            cmd="ping server1 > ${dir}/ping_${VM_NAME}_to_server1.log & "
            echolog ${cmd}
            eval ${cmd}
            pidping1=$!
            cmd="ping server2 > ${dir}/ping_${VM_NAME}_to_server2.log & "
            echolog ${cmd}
            eval ${cmd}
            pidping2=$!
            cmd="ping server3 > ${dir}/ping_${VM_NAME}_to_server3.log & "
            echolog ${cmd}
            eval ${cmd}
            pidping3=$!
            # run dstat
            cmd="dstat -tcnm --output ${dir}/dstat_${VM_NAME}.csv > ${dir}/dstat_${VM_NAME}.log & "
            echolog ${cmd}
            eval ${cmd}
            piddstat=$!
            # run middleware
            cmd="${cmdpart} > ${dir}/${VM_NAME}.log & "
            echolog ${cmd}
            eval ${cmd}
            pid=$!
            echolog "Sleeping $((time+20)) secs for the test to finish..."
            # kill all
            sleep $((time+20))
            echolog "Kill java, ping, dstat (if exist)"
            kill ${pid}
            kill -2 ${pidping1}
            kill -2 ${pidping2}
            kill -2 ${pidping3}
            kill ${piddstat}
            echolog "Sleeping 10 extra secs for logs to be fully dumped..."
            sleep 10
            echolog "Kill java again if exists"
            kill -9 ${pid}  # in case middleware mysteriously cannot stop
            echolog "------------------------------------------------------------------------------------------"
            echolog "Sharded test with multi-key-get=${key}, repetition $r end"
            echolog "=========================================================================================="
            echolog; echolog
        done
    done
fi

# 5.2 Non-sharded
fnamepart="../logs/5.2"
keys=(1 3 6 9)

echolog "# ASL section 5 experiments: 5.2 nonsharded multigets"
echolog "Run each experiment with ${time} seconds and ${repetitions} repetitions"

if [[ ${type} == "memtier" ]]; then
    # client side
    cmdpart="memtier_benchmark --port=11211 --protocol=memcache_text --expiry-range=99999-100000 --key-maximum=10000 --data-size=4096 --clients=2 --threads=1"
e
    echolog

    for key in "${keys[@]}"; do
        for r in `seq 1 ${repetitions}`; do
            echolog "============================================================================================"
            echolog "NonSharded test with multi-key-get=${key}, repetition $r begin"
            echolog "--------------------------------------------------------------------------------------------"
            echolog "Waiting for 10 secs for middleware to start..."
            sleep 10  # give middleware some time to start
            echolog "Run test (of ${time} secs) now"
            dir="${fnamepart}/k${key}/r${r}"
            mkdir -p ${dir}
            # run ping
            cmd1="ping middleware1 > ${dir}/ping_${VM_NAME}_to_middleware1.log & "
            echolog ${cmd1}
            eval ${cmd1}
            pidping1=$!
            cmd2="ping middleware2 > ${dir}/ping_${VM_NAME}_to_middleware2.log & "
            echolog ${cmd2}
            eval ${cmd2}
            pidping2=$!
            # run dstat
            cmd="dstat -tcnm --output ${dir}/dstat_${VM_NAME}.csv > ${dir}/dstat_${VM_NAME}.log & "
            echolog ${cmd}
            eval ${cmd}
            piddstat=$!
            # run memtier
            cmd1="${cmdpart} --multi-key-get=${key} --ratio=1:${key} --server=middleware1 --test-time=${time} > ${dir}/${VM_NAME}0.log 2>&1 &"
            cmd2="${cmdpart} --multi-key-get=${key} --ratio=1:${key} --server=middleware2 --test-time=${time} > ${dir}/${VM_NAME}1.log 2>&1 &"
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
            kill -2 ${pidping1}
            kill -2 ${pidping2}
            kill ${piddstat}
            echolog "Waiting for extra 10 secs for middleware to fully stop..."
            sleep 10  # give middleware to stop (dump statistics)
            echolog "------------------------------------------------------------------------------------------"
            echolog "NonSharded test with multi-key-get=${key}, repetition $r end"
            echolog "=========================================================================================="
            echolog; echolog
        done
    done
else
    # middleware side
    cmdpart="java -Dlog4j.configurationFile=../middleware/lib/log4j2.xml -cp ../middleware/dist/middleware-zhiyang.jar:../middleware/lib/* ch.ethz.asltest.RunMW -l 0.0.0.0 -p 11211 -t 64 -s false -m server1:11211 server2:11211 server3:11211"

    echolog

    for key in "${keys[@]}"; do
        for r in `seq 1 ${repetitions}`; do
            echolog "============================================================================================"
            echolog "NonSharded test with multi-key-get=${key}, repetition $r begin"
            echolog "--------------------------------------------------------------------------------------------"
            dir="${fnamepart}/k${key}/r${r}"
            mkdir -p ${dir}
            # run ping
            cmd="ping server1 > ${dir}/ping_${VM_NAME}_to_server1.log & "
            echolog ${cmd}
            eval ${cmd}
            pidping1=$!
            cmd="ping server2 > ${dir}/ping_${VM_NAME}_to_server2.log & "
            echolog ${cmd}
            eval ${cmd}
            pidping2=$!
            cmd="ping server3 > ${dir}/ping_${VM_NAME}_to_server3.log & "
            echolog ${cmd}
            eval ${cmd}
            pidping3=$!
            # run dstat
            cmd="dstat -tcnm --output ${dir}/dstat_${VM_NAME}.csv > ${dir}/dstat_${VM_NAME}.log & "
            echolog ${cmd}
            eval ${cmd}
            piddstat=$!
            # run middleware
            cmd="${cmdpart} > ${dir}/${VM_NAME}.log & "
            echolog ${cmd}
            eval ${cmd}
            pid=$!
            echolog "Sleeping $((time+20)) secs for the test to finish..."
            # kill all
            sleep $((time+20))
            echolog "Kill java, ping, dstat (if exist)"
            kill ${pid}
            kill -2 ${pidping1}
            kill -2 ${pidping2}
            kill -2 ${pidping3}
            kill ${piddstat}
            echolog "Sleeping 10 extra secs for logs to be fully dumped..."
            sleep 10
            echolog "Kill java again if exists"
            kill -9 ${pid}  # in case middleware mysteriously cannot stop
            echolog "------------------------------------------------------------------------------------------"
            echolog "NonSharded test with multi-key-get=${key}, repetition $r end"
            echolog "=========================================================================================="
            echolog; echolog
        done
    done
fi

echolog "# ASL section 5 experiments ALL DONE."
