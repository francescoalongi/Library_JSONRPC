# JSONRPC Library
This is an implementation of the JSON-RPC 2.0 protocol. It ensure the communication between a Client and a Server using ZeroMQ.

## Class diagram
In order to have a general view of the entire project, here is the class diagram.

![Class diagram](https://raw.githubusercontent.com/francescoalongi/Library_JSONRPC/master/uml/class_diagram.png)

As seen in the previous image, there are three packages. The "channel.library" package has an interface which can be implemented by any kind of messaging library.

## Usage
To know how to use this library, check the three java files ('ClassWithMethods', 'ClientMain', 'ServerMain') in the src folder.

## References
[JSON-RPC](https://www.jsonrpc.org/)

[ZeroMQ](http://zeromq.org/)
