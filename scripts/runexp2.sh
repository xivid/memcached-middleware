#!/usr/bin/env bash

if [ -z "${VM_NAME}" ]; then
    echo "Variable VM_NAME not set!"
    exit
fi

logname="runexp2_${VM_NAME}.log"
[ -e ${logname} ] && rm ${logname}

echolog() {
    echo "[runner]" `date +%Y-%m-%d\ %H:%M:%S` $@ | tee -a ${logname}
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


echolog "======================================================"
echolog "Run exp2.2"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == "client1" ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp2.2.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "Idle waiting on this machine."
fi
echolog "sleep 5000 for exp2.2 to finish"
echolog
echolog
sleep 5000
echolog "waked up from waiting for exp2.2, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog

echolog "======================================================"
echolog "Run exp2.1"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == client* ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp2.1.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "Idle waiting on this machine."
fi
echolog "sleep 5000 for exp2.1 to finish"
echolog
echolog
sleep 5000
echolog "waked up from waiting for exp2.1, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog


echolog "All experiments finished. ps:"
ps
