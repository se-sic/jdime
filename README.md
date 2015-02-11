# JDime  -  Structured Merge with Auto-Tuning
## System Requirements:
* gradle (http://www.gradle.org/)
* git (http://git-scm.com/)
* glpk (http://www.gnu.org/software/glpk/)
* gplk-java (http://glpk-java.sourceforge.net/)

__Debian/Ubuntu:__  
`apt-get install git glpk libglpk-java`

__Redhat/Fedora:__  
`yum install git glpk`  
glpk-java has to be installed manually

__Suse/OpenSuse:__  
`zypper install git glpk`  
glpk-java has to be installed manually  

__Windows:__  
Precompiled binaries (including the required .dll files for glpk and glpk-java) can be found at
(http://sourceforge.net/projects/winglpk/)

#### Installation:
Use `gradle -Pglpk=PATH -PglpkJava=PATH installApp` providing the paths to the `glpk_X_XX` and `glpk_X_XX_java` 
native libraries appropriate for your platform. `X_XX` refers to the version of glpk, currently __version 4.55__ is required.  

After running the command given above the directory `build/install/JDime/bin` will contain Unix and Windows scripts
that you can use to start the application.

#### Usage:
The input versions are passed to JDime as command line arguments. To perform a three-way merge, JDime is invoked as follows: 
`JDime -mode [unstructured|structured|autotuning] -output [directory] <leftVersion> <baseVersion> <rightVersion>`

