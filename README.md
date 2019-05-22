# J-RED

J-RED consists of J-RED Console, a [JavaEE-based](http://www.oracle.com/technetwork/java/javaee/overview/index.html) app deployed as a [Web Application Resource (WAR)](https://en.wikipedia.org/wiki/WAR_(file_format)), and a browser-based flow editor (J-RED Editor). The flow editor derives from [Node-RED](https://nodered.org), but, unlike Node-RED, the runtime and all nodes are implemented in Java.

![](https://github.com/nephele-tech/j-red/wiki/images/jred-console.png)
###### Figure 1. The J-RED Console - showing workspaces.

![](https://github.com/nephele-tech/j-red/wiki/images/jred-editor.png)
###### Figure 2. The J-RED Editor - showing the node palette (left), workspace (centre), info/debug sidebar (right).

## Quick start

To run J-RED Console container:

```
docker run -it -p 8888:8080 --name my-jred-console ntechnology/jred-console
```

This command will download the latest `ntechnology/jred-console` container from [DockerHub](https://cloud.docker.com/repository/docker/ntechnology/jred-console) and run an instance of it with the name `my-jred-console` and with port `8888` exposed. In the terminal window you will see J-RED Console startup logs. Once started you can then browse to `http://localhost:8888` to access the JRED Console. Create a _New Workspacee_ and navigate to it...

Hit `Ctrl-p` `Ctrl-q` to detach from the container. This leaves it running in the background.

To reattach to the container:

```
docker attach my-jred-console
```

To stop the container:

```
docker stop my-jred-console
```

To start the container

```
docker start my-jred-console
```

## Getting started

 * [Wiki](https://github.com/nephele-tech/j-red/wiki) - everything from first install to deploying flows.
