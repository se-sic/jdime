# JDime  -  Structured Merge with Auto-Tuning

[![Build Status](https://travis-ci.org/xai/jdime.svg?branch=develop)](https://travis-ci.org/xai/jdime)

## License & Copyright
* Copyright (C) 2013-2014 Olaf Lessenich  
* Copyright (C) 2014-2015 University of Passau, Germany  
> Authors: Olaf Lessenich, Georg Seibt

All rights reserved.  

JDime is covered by the GNU Lesser General Public License.  
The full license text is distributed with this software. See the `LICENSE` file.

## Tools Used:
JDime uses these tools/libraries:

* ExtendJ (https://bitbucket.org/jastadd/jastaddj)
> Commit [e77ceb1](https://bitbucket.org/extendj/extendj/src/e77ceb1)  
> Copyright (c) 2005-2008, Torbjörn Ekman  
> Copyright (c) 2005-2015, ExtendJ Committers  
> ExtendJ is covered by the Modified BSD License.  
> The full license text is distributed with this software.  
> See the file licenses/ExtendJ-BSD.

The changes that were made to ExtendJ are shipped with this software  
and covered by the Modified BSD License.  
See patches/ExtendJ for the changes,  
and patches/ExtendJ/LICENSE for the license text.

## System Requirements:
* git (http://git-scm.com/)

__Debian/Ubuntu:__  
`apt-get install git`

__Redhat/Fedora:__  
`yum install git`

__Suse/OpenSuse:__  
`zypper install git`

## Installation:
Clone the repository using `git clone $URL`.

JDime uses gradle as a build system.
To avoid version mismatches with already installed instances of gradle, you can use the supplied gradle wrapper `gradlew` that bootstraps the right version of gradle automatically.


After running `./gradlew installDist`, the directory `build/install/JDime/bin` will contain Unix and Windows scripts that you can use to start the application.

## Usage:
The input versions are passed to JDime as command line arguments. To perform a three-way merge, JDime is invoked as follows: 

`JDime --mode [unstructured|structured|autotuning] --output [file/directory] <leftVersion> <baseVersion> <rightVersion>`

Run `JDime --help` to show more extensive usage information.
