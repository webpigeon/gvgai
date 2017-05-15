GVGAI - Android Version
=======================

This repository is a version of the [GVG-AI][gvgai-repo] framework ported to maven and
with dependencies on AWT removed from the code.

## Where is the upstream for this project?
* The upstream for this project is located [here][gvgai-repo].
* Work on the maven version is being carried out on FOSS Galaxy's [gitlab server][fossgalaxy-vgdl].


## Build instructions
### Build dependencies
In order to build a project using Maven, ensure you have both Maven and Java 1.8.

```bash
# Debian based (including ubuntu)
sudo apt install openjdk-8-jdk maven

# Fedora (untested)
sudo dnf install java-1.8.0-openjdk maven

# OS X (untested)
brew install maven
```

### Building the project
In order to build the project, you can use Maven:

```bash
mvn clean package
```

This will generate the JAR in target/

[gvgai-repo]: https://github.com/EssexUniversityMCTS/gvgai
[fossgalaxy-vgdl]: https://git.fossgalaxy.com/iggi/vgdl