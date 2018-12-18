#!/usr/bin/env bash

[ -e runexp2.2_and_3.log ] && rm runexp2.2_and_3.log

echolog() {
    echo "[runner]" `date +%Y-%m-%d\ %H:%M:%S` $@ | tee -a runexp2.2_and_3.log
}

if [ -z "${VM_NAME}" ]; then
    echolog "Variable VM_NAME not set!"
    exit
else
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
fi


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
echolog "sleep 4050s for exp2.2 to finish"
echolog
echolog
sleep 4050
echolog "waked up from waiting for exp2.2, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog

echolog "======================================================"
echolog "Run exp3.2"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == client* ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp3.2.sh &"
    echolog $cmd
    eval $cmd
elif [[ ${VM_NAME} == mw* ]]; then
    echolog "Run middleware on this machine."
    cmd="./exp3.2.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "Idle waiting on this machine."
fi
echolog "sleep 18800s for exp3.2 to finish"
echolog
echolog
sleep 18800
echolog "waked up from waiting for exp3.2, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog

echolog "======================================================"
echolog "Run exp3.1"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == client* ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp3.1.sh &"
    echolog $cmd
    eval $cmd
elif [[ ${VM_NAME} == "mw1" ]]; then
    echolog "Run middleware on this machine."
    cmd="./exp3.1.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "shutting down this machine."
    sudo poweroff
fi
echolog "sleep 18800s for exp3.1 to finish"
echolog
echolog
sleep 18800
echolog "waked up from waiting for exp3.1, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog


echolog "All experiments finished. ps:"
ps
