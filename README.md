# ListServer
This repository is for the [List](https://github.com/HoldenErnest/List) application but it is also used as a ground to create my LupuServer as it will need setup before the project can be underway.

Read [notes](notes.md) for more information on startup of the System as a whole

## Setup
- Clone the repo
- Install Java JDK
- Get an SSL Cert and transfer it into a java keystore
- Find all files to compile `find -name "*.java" > compileIt.txt`
- Compile them `javac @compileIt.txt`

## Running
- `java Server <port> <rootDir>`
- connect using my List application

## Goals:
- get ssh working ✔
- get certbot working ✔
- get apache working? ✔
- generate a working ssl socket ✔
- generate a client in js that can connect to this ✔
- configure the server to accept specific requests from the client ✔
- optimize the protocol, Read the [notes](notes.md)
- optimize the client
- dockerize it all?
