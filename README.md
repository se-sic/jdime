# JDime  
**Structured Merge with Auto-Tuning**

## License & Copyright
* Copyright (C) 2013-2014 Olaf Lessenich
* Copyright (C) 2014-2017 University of Passau, Germany
> Authors: Olaf Lessenich, Georg Seibt

All rights reserved.

JDime is covered by the GNU Lesser General Public License.
The full license text is distributed with this software. See the `LICENSE` file.

## Tools Used:
JDime uses these tools/libraries:

* ExtendJ (https://bitbucket.org/jastadd/jastaddj)
> Commit [85fe215](https://bitbucket.org/extendj/extendj/commits/85fe215542d5cde4753e10a2b068b394f79d7984)
> Copyright (c) 2005-2008, Torbjörn Ekman
> Copyright (c) 2005-2017, ExtendJ Committers
> ExtendJ is covered by the Modified BSD License.
> The full license text is distributed with this software.
> See the file licenses/ExtendJ-BSD.

The changes that were made to ExtendJ are shipped with this software
and covered by the Modified BSD License.
See patches/ExtendJ for the changes,
and patches/ExtendJ/LICENSE for the license text.

## System Requirements:
* At least Java 11 for running the Gradle build of JDime
* Java 8 for developing / running JDime. If you are using Gradle to build / run, Gradle will automatically discover / download Java 8.
* If you are not on 64bit Linux or Windows: [libgit2](https://libgit2.github.com/) (for [JNativeMerge](https://gitlab.infosun.fim.uni-passau.de/seibt/JNativeMerge))

## Installation:
Clone the repository using `git clone $URL`.

JDime uses [Gradle](https://gradle.org/) as its build system.
To avoid version mismatches with already installed instances of gradle, you can use the supplied gradle wrapper `gradlew` that bootstraps the right version of gradle automatically.

After running `./gradlew installDist`, the directory `build/scripts` will contain Unix and Windows scripts that you can use to start the application.

## Usage:
The input versions are passed to JDime as command line arguments. To perform a three-way merge, JDime may be invoked as follows:

`JDime --mode [linebased|semistructured|structured] --output [file/directory] <leftVersion> <baseVersion> <rightVersion>`

**Run `JDime --help` to show more extensive usage information.**
