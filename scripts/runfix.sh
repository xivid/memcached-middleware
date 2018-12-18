#!/usr/bin/env bash

[ -e runfix.log ] && rm runfix.log

echolog() {
    echo "[runner]" `date +%Y-%m-%d\ %H:%M:%S` $@ | tee -a runfix.log
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
echolog "Run exp4"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == client* ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp4.sh &"
    echolog $cmd
    eval $cmd
elif [[ ${VM_NAME} == mw* ]]; then
    echolog "Run middleware on this machine."
    cmd="./exp4.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "Idle waiting on this machine."
fi
echolog "sleep 11000s for exp4.1 to finish"
echolog
echolog
sleep 11000
echolog "waked up from waiting for exp4.1, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog

echolog "======================================================"
echolog "Run exp4_fix2"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == client* ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp4_fix2.sh &"
    echolog $cmd
    eval $cmd
elif [[ ${VM_NAME} == mw* ]]; then
    echolog "Run middleware on this machine."
    cmd="./exp4_fix2.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "Idle waiting on this machine."
fi
echolog "sleep 4100 for exp4_fix2 to finish"
echolog
echolog
sleep 4100
echolog "waked up from waiting for exp4_fix2, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog


echolog "======================================================"
echolog "Run exp3.2 writeonly"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == client* ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp3.2_write.sh &"
    echolog $cmd
    eval $cmd
elif [[ ${VM_NAME} == mw* ]]; then
    echolog "Run middleware on this machine."
    cmd="./exp3.2_write.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "shutting down this machine."
fi
echolog "sleep 11000 for exp3.2 to finish"
echolog
echolog
sleep 11000
echolog "waked up from waiting for exp3.2, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog

echolog "======================================================"
echolog "Run exp3.2 writeonly fix2"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == client* ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp3.2_write_fix2.sh &"
    echolog $cmd
    eval $cmd
elif [[ ${VM_NAME} == mw* ]]; then
    echolog "Run middleware on this machine."
    cmd="./exp3.2_write_fix2.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "shutting down this machine."
fi
echolog "sleep 2900 for exp3.2 writeonly fix2 to finish"
echolog
echolog
sleep 2900
echolog "waked up from waiting for exp3.2 writeonly fix2, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog


echolog "======================================================"
echolog "Run exp3.1 writeonly"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == client* ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp3.1_write.sh &"
    echolog $cmd
    eval $cmd
elif [[ ${VM_NAME} == "mw1" ]]; then
    echolog "Run middleware on this machine."
    cmd="./exp3.1_write.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "shutting down this machine."
fi
echolog "sleep 11000 for exp3.1 to finish"
echolog
echolog
sleep 11000
echolog "waked up from waiting for exp3.1, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog

echolog "======================================================"
echolog "Run exp3.1 writeonly fix"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == client* ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp3.1_write_fix.sh &"
    echolog $cmd
    eval $cmd
elif [[ ${VM_NAME} == "mw1" ]]; then
    echolog "Run middleware on this machine."
    cmd="./exp3.1_write_fix.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "shutting down this machine."
fi
echolog "sleep 4100 for exp3.1 writeonly fix to finish"
echolog
echolog
sleep 4100
echolog "waked up from waiting for exp3.1 writeonly fix, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog


echolog "All experiments finished. ps:"
ps
