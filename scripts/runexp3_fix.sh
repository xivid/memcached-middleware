#!/usr/bin/env bash
if [ -z "${VM_NAME}" ]; then
    echolog "Variable VM_NAME not set!"
    exit
fi

logname="runexp3_fix_${VM_NAME}.log"
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
echolog "Run exp3.2 readonly fix"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == client* ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp3.2_read_fix.sh &"
    echolog $cmd
    eval $cmd
elif [[ ${VM_NAME} == mw* ]]; then
    echolog "Run middleware on this machine."
    cmd="./exp3.2_read_fix.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "Idle waiting on this machine."
fi
echolog "sleep 1400 for exp3.2 readonly fix to finish"
echolog
echolog
sleep 1400
echolog "waked up from waiting for exp3.2 readonly fix, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog

echolog "======================================================"
echolog "Run exp3.1 readonly fix"
echolog "------------------------------------------------------"
if [[ ${VM_NAME} == client* ]]; then
    echolog "Run memtier on this machine."
    cmd="./exp3.1_read_fix.sh &"
    echolog $cmd
    eval $cmd
elif [[ ${VM_NAME} == "mw1" ]]; then
    echolog "Run middleware on this machine."
    cmd="./exp3.1_read_fix.sh &"
    echolog $cmd
    eval $cmd
else
    echolog "shutting down this machine."
fi
echolog "sleep 720 for exp3.1 readonly fix to finish"
echolog
echolog
sleep 720
echolog "waked up from waiting for exp3.1 readonly fix, ps:"
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
    echolog "Idle waiting on this machine."
fi
echolog "sleep 10620 for exp3.2 writeonly to finish"
echolog
echolog
sleep 10620
echolog "waked up from waiting for exp3.2 writeonly, ps:"
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
    echolog "Idle waiting on this machine."
fi
echolog "sleep 6660 for exp3.1 writeonly fix to finish"
echolog
echolog
sleep 6660
echolog "waked up from waiting for exp3.1 writeonly fix, ps:"
echolog `ps`
echolog "======================================================"
echolog
echolog


echolog "All experiments finished. ps:"
ps
