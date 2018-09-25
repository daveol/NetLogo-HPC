[![Build Status](https://travis-ci.org/daveol/NetLogo-HPC.svg?branch=master)](https://travis-ci.org/daveol/NetLogo-HPC)
# NetLogo-HPC
Creating a application to run Netlogo on HPC clusters

## Building the application
Building an application bundle
```bash
./sbt packArchive
```

Which will create the file ```target/NetLogo-HPC-*.tar.gz``` which contains the dependencies to run NetLogo-HPC. This
 be unpacked by running```tar xvf /path/to/NetLogo-HPC-*.tar.gz``` which then makes ```bin/netlogo-hpc``` which runs the
 application.
 
