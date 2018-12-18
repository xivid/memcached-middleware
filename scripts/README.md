# Scripts

Experiment scripts. Each bash script named after `exp*.sh` runs the experiments of one or more sections. Those marked with "fix" are additional experiments e.g. to determine the meaningful VC ranges. All other scripts are unnecessary helper scripts, including:

- `prepopulate.sh`: prepopulate all servers with uniform key pattern and one client only, to cover the whole key range as fast as possible.
- `iperf.sh`: used to test the bandwidth between different virtual machines.