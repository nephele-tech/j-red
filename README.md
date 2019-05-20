# J-RED

J-RED consists of a [JavaEE-based](http://www.oracle.com/technetwork/java/javaee/overview/index.html) runtime deployed as a [Web Application Resource (WAR)](https://en.wikipedia.org/wiki/WAR_(file_format)), and a browser-based flow editor. The flow editor derives from [Node-RED](https://nodered.org), but, unlike Node-RED, the runtime and all J-RED nodes are implemented in Java.

## Installation

Before you can install J-RED WAR, you must have a working installation of a JavaEE container like [Apache Tomcat](https://tomcat.apache.org/download-90.cgi).

Download the latest release of [J-RED](https://github.com/nephele-tech/j-red/releases), rename it to `jred-editor.war`, and copy it to `$CATALINA_BASE/webapps` folder. Start Tomcat and point a local browser at `http://localhost:8080/jred-editor`.

## Getting started

 * [Wiki](https://github.com/nephele-tech/j-red/wiki) - everything from first install to deploying flows.
