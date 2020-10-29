Guide
----------------

## Development Setup(Ubuntu)

### Prerequisites

- Install  [[JDK8](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html )] 
  
  `$ sudo apt install openjdk-8-jdk git make gtkwave`


- Install  [[sbt](https://www.scala-sbt.org/ )]

```
  $ echo "deb https://dl.bintray.com/sbt/debian /" | \
  	sudo tee -a /etc/apt/sources.list.d/sbt.list
```
```
  $ curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
```
```
  $ sudo apt-get update
```
```
  $ sudo apt-get install sbt
```
### Setting Up a Project
 - Initialize  sbt
    `$ cd ./sbt`
    `$ vim repositories`
    
 - Paste the following (be sure to add a new line after [repositories]

    `[repositories]`

    `maven-central: https://repo1.maven.org/maven2/`

 - Clone
	`gitclone https://github.com/shuosc/shuorv.git`
	
 - Run the test
```
  $ sbt
  $ compile
  $ test
```
 - Check the waveform

	`cd test_run_dir`
	
	You can use [[GTKWave]](http://gtkwave.sourceforge.net/ ) to open `.vcd` to check the waveform
	
