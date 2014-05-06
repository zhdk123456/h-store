#!/bin/sh
ant clean-java build-java
ant hstore-prepare -Dproject=microwintimehstorefull -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=microwintimehstorecleanup -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=microwintimehstoresomecleanup -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=microwintimehstorenocleanup -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=microwintimesstore -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=microwinhstorefull -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=microwinhstorecleanup -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=microwinhstoresomecleanup -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=microwinhstorenocleanup -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=microwinsstore -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=voterdemohstore -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=voterdemosstore -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=voterdemohstorenocleanup -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=voterwinhstore -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=voterwinsstore -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=voterwintimehstore -Dhosts="localhost:0:0"
ant hstore-prepare -Dproject=voterwintimesstore -Dhosts="localhost:0:0"
python ./tools/runexperiments.py --tmin 1 --tmax 1 --tstep 1 --rmin 1000 --rmax 25000 --rstep 1000 --stop --warmup 10000 -p microwinhstorefull -o "experiments/0505/site08/microwinhstorefull-1c-10w10s-0505.txt"
python ./tools/runexperiments.py --tmin 1 --tmax 1 --tstep 1 --rmin 1000 --rmax 25000 --rstep 1000 --stop --warmup 10000 -p microwinsstore -o "experiments/0505/site08/microwinsstore-1c-10w10s-0505.txt"
python ./tools/runexperiments.py --tmin 1 --tmax 1 --tstep 1 --rmin 1000 --rmax 25000 --rstep 1000 --stop --warmup 40000 -p microwintimehstorefull -o "experiments/0505/site08/microwintimehstorefull-1c-10w2s1000t-0505.txt"
python ./tools/runexperiments.py --tmin 1 --tmax 1 --tstep 1 --rmin 1000 --rmax 25000 --rstep 1000 --stop --warmup 40000 -p microwintimesstore -o "experiments/0505/site08/microwintimesstore-1c-10w2s1000t-0505.txt"
python ./tools/runexperiments.py --tmin 1 --tmax 1 --tstep 1 --rmin 1000 --rmax 15000 --rstep 1000 --stop --warmup 40000 -p voterdemohstore -o "experiments/0505/site08/voterdemohstore-1c-10w2s1000t-0505.txt"
python ./tools/runexperiments.py --tmin 1 --tmax 1 --tstep 1 --rmin 1000 --rmax 15000 --rstep 1000 --stop --warmup 40000 -p voterdemohstorenocleanup -o "experiments/0505/site08/voterdemohstorenocleanup-1c-10w2s1000t-0505.txt"
python ./tools/runexperiments.py --tmin 1 --tmax 1 --tstep 1 --rmin 1000 --rmax 15000 --rstep 1000 --stop --warmup 40000 -p voterdemosstore -o "experiments/0505/site08/voterdemosstore-1c-10w2s1000t-0505.txt"
python ./tools/runexperiments.py --tmin 1 --tmax 1 --tstep 1 --rmin 1000 --rmax 15000 --rstep 1000 --stop --warmup 10000 -p voterwinhstore -o "experiments/0505/site08/voterwinhstore-1c-10w10s-0505.txt"
python ./tools/runexperiments.py --tmin 1 --tmax 1 --tstep 1 --rmin 1000 --rmax 15000 --rstep 1000 --stop --warmup 10000 -p voterwinsstore -o "experiments/0505/site08/voterwinhstore-1c-10w10s-0505.txt"
python ./tools/runexperiments.py --tmin 1 --tmax 1 --tstep 1 --rmin 1000 --rmax 15000 --rstep 1000 --stop --warmup 40000 -p voterwintimehstore -o "experiments/0505/site08/voterwintimehstore-1c-10w2s1000t-0505.txt"
python ./tools/runexperiments.py --tmin 1 --tmax 1 --tstep 1 --rmin 1000 --rmax 15000 --rstep 1000 --stop --warmup 40000 -p voterwintimesstore -o "experiments/0505/site08/voterwintimesstore-1c-10w2s1000t-0505.txt"
python ./tools/runexperiments.py --tmin 10 --tmax 10 --tstep 1 --rmin 100 --rmax 1500 --rstep 100 --stop --warmup 40000 -p voterdemohstore -o "experiments/0505/site08/voterdemohstore-10c-10w2s1000t-0505.txt"
python ./tools/runexperiments.py --tmin 10 --tmax 10 --tstep 1 --rmin 100 --rmax 1500 --rstep 100 --stop --warmup 40000 -p voterdemohstorenocleanup -o "experiments/0505/site08/voterdemohstorenocleanup-10c-10w2s1000t-0505.txt"
python ./tools/runexperiments.py --tmin 10 --tmax 10 --tstep 1 --rmin 100 --rmax 1500 --rstep 100 --stop --warmup 40000 -p voterdemosstore -o "experiments/0505/site08/voterdemosstore-10c-10w2s1000t-0505.txt"
