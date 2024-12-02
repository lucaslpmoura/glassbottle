# Glassbottle: A simple chatroom application

Glassbotle is a client/server chatroom built as a proof of concept, using a REST API to communicate the server with the client.

## Dependecies
### Server
- NodeJS
- Express

### Client
- Java SDK 21
- Maven
- HttpClient
  
If everything is in order, npm and Maven should setup everything you need.

## Exectuing
### Server

```console

node server/server.js

```

### Client

You can use the provided .jar:

```console

java -jar client/glassbottle/target/glassbottle-1.0-SNAPSHOT.jar

```

or you can compile it yourself:

```console

cd client/glassbottle
mvn install
mvn compile

```

## Bugs

- Users need to have different nicknames;

## Future Improvements

- Admin Console;
- Password protected rooms;
- Better Interface