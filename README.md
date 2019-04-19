[![Build Status](https://travis-ci.org/daveol/NetLogo-HPC.svg?branch=master)](https://travis-ci.org/daveol/NetLogo-HPC)
[![Maintainability](https://api.codeclimate.com/v1/badges/97b12d3a30e4b74b26f8/maintainability)](https://codeclimate.com/github/daveol/NetLogo-HPC/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/97b12d3a30e4b74b26f8/test_coverage)](https://codeclimate.com/github/daveol/NetLogo-HPC/test_coverage)

# NetLogo-HPC
Application to run Netlogo on HPC clusters which use SLURM for scheduling. Uses by default NetLogo 6.0.4.

## Installing the application
1. Assuming you are on a linux based HPC
1. Change into the directory containing your model.
1. [Download the tar.gz file](https://github.com/daveol/NetLogo-HPC/releases) You can do that directly from the command line with ```wget  https://github.com/daveol/NetLogo-HPC/releases/download/0.1.4/NetLogo-HPC-0.1.4.tar.gz```
1. Unpack the archive with ```tar -zxvf NetLogo-HPC-0.1.4.tar.gz```

## Using the application
1. Change into the directory containing your model.
1. Start screen or tmux program. If you are not familiar with screen and what it does, please see the [wikipedia page on  GNU screen](https://en.wikipedia.org/wiki/GNU_Screen) 
1. execute ```netlogo-hpc --model model.nlogo --experiment experiment --table results.csv ```
1. You can now detach from your session, and wait for the simulations to finish. If you would like to be notified by email when it finishes or crashes, you can use the following incantation :
```netlogo-hpc --model model.nlogo --experiment experiment --table results.csv &&  mail -s "Simulation finished" user@example.com < /dev/null || mail -s "Simulation crashed" user@example.com < /dev/null ```

## Using extensions
1. Create a 'extensions' directory in the model experiment, and place any extensions you need there. Please note that at the moment not all extensions work. 

Knwn not to work : 
* Table


## Building the application
Please note, building has only been tested on Linux, but will probaly work on other unix type OSs, like OSX.

Building an application bundle
```bash
./sbt packArchive
```

Which will create the file ```target/NetLogo-HPC-*.tar.gz``` which contains the dependencies to run NetLogo-HPC. This
 be unpacked by running```tar xvf /path/to/NetLogo-HPC-*.tar.gz``` which then makes ```bin/netlogo-hpc``` which runs the
 application.


## Using with NetLogo versions other than 6.0.4
By default, netlogo-hpc uses NetLogo 6.04 for its internal working. If you wish to use a older (or newer) version, change 
```
libraryDependencies += "org.nlogo" % "netlogo" % "6.0.4"
```
in the build.sbt file to the desired version. Please note that the version you provide in the file must match the GitHub version tag.


## Documentation for netlogo-hpc
```
 Arguments:
  --help
      shows this help information

  --model [file]
      Specify the NetLogo model file

  --experiment [name]
      Specify the experiment to run

  --table [file]
      Table to write the metrics from simulations to

Advanced options:
  --application-id [string] (optional)
      Specifies the application id to check the network against, it will default to a random uuid

  --client-id [string] (optional)
      Specifies the client id to use for receiving and sending messages, will override to 'controller'
      when the controller role is specified otherwhise it will default to a random uuid

  --worker
      Manualy creates a worker that  conncets to the controller. You will not need this during normal use.
      
  --controller
      Default mode, creates a controller node first, then spawns the workers.

--connect [uri] or --listen [uri]
      Connect or Bind to a URI in the style of 'tcp://[ip]:[port]'

 Scheduler arguments:
  --concurrent [number of allowed tasks] (optional)
      Specifies the amount of tasks that are allowed to be concurrently deployed on the cluster, it defaults to eight tasks.
```
