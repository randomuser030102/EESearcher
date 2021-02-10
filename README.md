# ITGS IA Final Product

This is the source code of the ITGS final product.
<br>
# Compilation
This project uses [Apache Maven](https://maven.apache.org/) and requires [Java Runtime 11](https://adoptopenjdk.net/).
To compile the product, execute `./mvnw` A compiled binary specific to the operating system you are on will be generated under `target/EESearcher-1.0-SNAPSHOT.jar`
Alternatively, precompiled binaries can be found [here](https://github.com/randomuser030102/EESearcher/releases/tag/1.0-SNAPSHOT).

# Usage
The compiled jar can be opened via a double click; however, it is suggested to launch the jar via command line with these arguments: <br>
`java -jar pathToJar.jar -Xss1M`. This will ensure there is sufficient stack size for the regex to run properly. 

# System Requirements
The program requires a minimum of 128MB of memory. The program pre-bundles OpenJFX 11 (Java FX) and should work on Mac, Windows 10 and most flavours of linux. <br>
The app has only been tested on Mac and Windows 10. 
