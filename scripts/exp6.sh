#!/usr/bin/env bash
# 2k experiments: 3 clients, 1/2 middlewares (8/32 threads), 1/3 servers
# usage: ./exp6.sh [time] [repetitions] [nopop]

if [ -z "${VM_NAME}" ]; then
    echo "Variable VM_NAME not set!"
    exit
fi

# logging instrumentations
logfile="exp6_${VM_NAME}.log"

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

fnamepart="../logs/6"
workloads=("readonly" "writeonly")
middlewares=(2 1)
servers=(3 1)
workers=(8 32)

echolog "# ASL section 6 experiments"
echolog "Run each experiment with ${time} seconds and ${repetitions} repetitions"

# pre-populate the memcached servers
if [[ $3 != "nopop" ]]; then
    echolog "Populating server: server1 server2 server3"
    ./prepopulate.sh 30 server1 server2 server3
    echolog
fi

[ -e ${fnamepart} ] && backup="../logs/6/backup_$(date +%Y-%m-%d_%H-%M-%S)" && echolog "Old data folder found, renaming to ${backup}" && mv ${fnamepart} ${backup}
echolog

for m in "${middlewares[@]}"; do
    echolog "# ${m} middleware"

    for workload in "${workloads[@]}"; do
        # determine SET:GET ratio
        if [[ ${workload} == "readonly" ]]; then
            ratio="0:1"
        else
            ratio="1:0"
        fi
        for s in "${servers[@]}"; do
        for w in "${workers[@]}"; do
        for r in `seq 1 ${repetitions}`; do
            echolog "==========================================================================================="
            echolog "Test ${workload} with middlewares = $m, servers = $s, MW workers = $w, repetition $r begin"
            echolog "-------------------------------------------------------------------------------------------"

            dir="${fnamepart}/${workload}/m${m}/s${s}/w${w}/r${r}"
            mkdir -p ${dir}

            if [[ ${type} == "memtier" ]]; then
                #client side
                cmdpart="memtier_benchmark --port=11211 --protocol=memcache_text --expiry-range=99999-100000 --key-maximum=10000 --data-size=4096"

                echolog "Waiting for 10 secs for middleware to start..."
                sleep 10  # give middleware some time to start
                # run ping
                cmd1="ping middleware1 > ${dir}/ping_${VM_NAME}_to_middleware1.log & "
                echolog ${cmd1}
                eval ${cmd1}
                pidping1=$!
                if [[ ${m} == "2" ]]; then
                    cmd2="ping middleware2 > ${dir}/ping_${VM_NAME}_to__to_middleware2.log & "
                    echolog ${cmd2}
                    eval ${cmd2}
                    pidping2=$!
                fi
                # run dstat
                cmd="dstat -tcnm --output ${dir}/dstat_${VM_NAME}.csv > ${dir}/dstat_${VM_NAME}.log & "
                echolog ${cmd}
                eval ${cmd}
                piddstat=$!
                # run memtier
                if [[ ${m} == "2" ]]; then
                    cmd1="${cmdpart} --ratio=${ratio} --test-time=${time} --clients=32 --threads=1 --server=middleware1 > ${dir}/${VM_NAME}0.log 2>&1 & "
                    cmd2="${cmdpart} --ratio=${ratio} --test-time=${time} --clients=32 --threads=1 --server=middleware2 > ${dir}/${VM_NAME}1.log 2>&1 & "
                    echolog ${cmd1}
                    eval ${cmd1}
                    pid1=$!
                    echolog ${cmd2}
                    eval ${cmd2}
                    pid2=$!
                else
                    cmd1="${cmdpart} --ratio=${ratio} --test-time=${time} --clients=32 --threads=2 --server=middleware1 > ${dir}/${VM_NAME}1.log 2>&1 & "
                    echolog ${cmd1}
                    eval ${cmd1}
                    pid1=$!
                fi
                echolog "Waiting for $((time+10)) secs for test to fully stop..."
                sleep $((time+10))
                echolog "Kill memtier, ping and dstat (if exist)"
                kill ${pid1}
                if [[ ${m} == "2" ]]; then
                    kill ${pid2}
                fi
                kill -2 ${pidping1}
                if [[ ${m} == "2" ]]; then
                    kill -2 ${pidping2}
                fi
                kill ${piddstat}
                echolog "Waiting for extra 10 secs for middleware to fully stop..."
                sleep 10  # give middleware to stop (dump statistics)

            elif [[ ${m} == "2" || ${VM_NAME} == "mw1" ]]; then  # I am a middleware; we need two middlewares || (one middleware and I am mw1)
                # middleware side
                if [[ ${s} == "3" ]]; then
                    cmdpart="java -Dlog4j.configurationFile=../middleware/lib/log4j2.xml -cp ../middleware/dist/middleware-zhiyang.jar:../middleware/lib/* ch.ethz.asltest.RunMW -l 0.0.0.0 -p 11211 -s false -m server1:11211 server2:11211 server3:11211"
                else
                    cmdpart="java -Dlog4j.configurationFile=../middleware/lib/log4j2.xml -cp ../middleware/dist/middleware-zhiyang.jar:../middleware/lib/* ch.ethz.asltest.RunMW -l 0.0.0.0 -p 11211 -s false -m server1:11211"
                fi
                # run ping
                cmd1="ping server1 > ${dir}/ping_${VM_NAME}_to_server1.log & "
                echolog ${cmd1}
                eval ${cmd1}
                pidping1=$!
                if [[ ${s} == "3" ]]; then
                    cmd2="ping server2 > ${dir}/ping_${VM_NAME}_to_server2.log & "
                    echolog ${cmd2}
                    eval ${cmd2}
                    pidping2=$!
                    cmd3="ping server3 > ${dir}/ping_${VM_NAME}_to_server3.log & "
                    echolog ${cmd3}
                    eval ${cmd3}
                    pidping3=$!
                fi
                # run dstat
                cmd="dstat -tcnm --output ${dir}/dstat_${VM_NAME}.csv > ${dir}/dstat_${VM_NAME}.log & "
                echolog ${cmd}
                eval ${cmd}
                piddstat=$!
                # run memtier
                cmd="${cmdpart} -t ${w} > ${dir}/${VM_NAME}.log & "
                echolog ${cmd}
                eval ${cmd}
                pid=$!
                # kill all
                echolog "Sleeping $((time+20)) secs for the test to finish..."
                sleep $((time+20))
                echolog "Kill java, ping, dstat (if exist)"
                kill ${pid}
                kill -2 ${pidping1}
                if [[ ${s} == "3" ]]; then
                    kill -2 ${pidping2}
                    kill -2 ${pidping3}
                fi
                kill ${piddstat}
                echolog "Sleeping 10 extra secs for logs to be fully dumped..."
                sleep 10
                echolog "Kill java again if exists"
                kill -9 ${pid}  # in case middleware mysteriously cannot stop
            else
                echolog "Idle waiting for $((time+30)) secs on this machine"
                sleep $((time+30))
            fi

            echolog "-------------------------------------------------------------------------------------------"
            echolog "Test ${workload} with middlewares = $m, servers = $s, MW workers = $w, repetition $r end"
            echolog "==========================================================================================="
            echolog; echolog
        done
        done
        done
    done
done

echolog "# ASL section 6 experiments ALL DONE."

