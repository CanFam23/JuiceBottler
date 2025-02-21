# JuiceBottler Lab
## Overview
This project simulates a multi-threaded orange plant in which multiple workers process oranges into orange juice bottles using Java and Apache Ant. 
The simulation utilizes data and task parallelization to process oranges efficiently. In my implementation, I have 2 Plants running, and each plant has 6 threads peeling oranges,
4 squeezing oranges, and 3 bottling them. After testing, I found these numbers resulted in minimal waste and high efficiency. I used the [LinkedBlockingQueue](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/LinkedBlockingQueue.html)
 data structure to safely pass oranges between threads.

## Built with
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Apache Ant](https://img.shields.io/badge/Apache%20Ant-A81C7D?style=for-the-badge&logo=apache&logoColor=white)

## Getting started
* Open a terminal
* If Ant isn't installed and you have a mac with homebrew, run
```bash
brew install ant
```
If you don't have a mac with homebrew, sorry.
### Clone the repository
```bash
git clone https://github.com/CanFam23/JuiceBottler/
```
### Build and Run with Ant
1. Navigate to the repository
```bash
cd /filePath/JuiceBottler
```
2. Run with Ant
Run the program using
```bash
ant run
```
Other helpful commands:
```bash
ant clean # Deletes old compiled files.
ant compile # Compiles Java source files into bin/
```

**Sample Output**
```bash
Buildfile: /file/path/to/project/classes/JuiceBottler/build.xml

init:

compile:
    [javac] Compiling 3 source files to /file/path/to/project/classes/JuiceBottler/dist/classes

jar:
      [jar] Building jar: /file/path/to/project/classes/JuiceBottler/dist/Plant.jar

run:
     [java] Worker[1.6] is working.
     [java] Worker[1.2] is working.
     [java] Worker[1.7] is working.
     [java] Worker[1.1] is working.
     [java] Worker[1.3] is working.
     [java] Worker[1.4] is working.
     [java] Plant[1] Processing oranges
     [java] Worker[1.5] is working.
     [java] Worker[1.8] is working.
     [java] Worker[2.1] is working.
     [java] Worker[1.9] is working.
     [java] Worker[1.10] is working.
     [java] Worker[1.11] is working.
     [java] Worker[1.12] is working.
     [java] Worker[2.4] is working.
     [java] Worker[1.13] is working.
     [java] Plant[2] Processing oranges
     [java] Worker[2.2] is working.
     [java] Worker[2.3] is working.
     [java] Worker[2.5] is working.
     [java] Worker[2.6] is working.
     [java] Worker[2.7] is working.
     [java] Worker[2.8] is working.
     [java] Worker[2.9] is working.
     [java] Worker[2.11] is working.
     [java] Worker[2.10] is working.
     [java] Worker[2.12] is working.
     [java] Worker[2.13] is working.
     [java]  
     [java]  
     [java] Plant[2] Done
     [java] Worker[2.9] has finished working.
     [java] Worker[1.10] has finished working.
     [java] Worker[2.3] has finished working.
     [java] Plant[1] Done
     [java] Worker[1.3] has finished working.
     [java] Worker[1.13] has finished working.
     [java] Worker[2.13] has finished working.
     [java] Worker[1.7] has finished working.
     [java] Worker[2.10] has finished working.
     [java] Worker[1.11] has finished working.
     [java] Worker[1.9] has finished working.
     [java] Worker[2.11] has finished working.
     [java] Worker[2.7] has finished working.
     [java] Worker[2.5] has finished working.
     [java] Worker[1.4] has finished working.
     [java] Worker[1.8] has finished working.
     [java] Worker[2.8] has finished working.
     [java] Worker[1.12] has finished working.
     [java] Worker[2.12] has finished working.
     [java] Worker[2.1] has finished working.
     [java] Worker[1.2] has finished working.
     [java] Worker[2.2] has finished working.
     [java] Worker[1.1] has finished working.
     [java] Worker[1.5] has finished working.
     [java] Worker[2.6] has finished working.
     [java] Worker[1.6] has finished working.
     [java] Worker[2.4] has finished working.
     [java] Total provided/processed = 564/558
     [java] Total left in queues = 6
     [java] Total leftover after bottling oranges = 0
     [java] Total removed from queues = 0
     [java] Created 186, wasted 6 oranges

BUILD SUCCESSFUL
Total time: 6 seconds
```
