/*
Glassbottle Server
Created by Lucas Moura, 2024

A simple chat room server. Users can join existing rooms, or create new ones.
Currently, the default values must be altered before running.
*/

const express = require('express');

var server = express();
server.use(express.json());
server.set('trust proxy', true)
const PORT = 5000;

const MAX_ROOMS = 4;
const MAX_USERS = 10;

var currentUsers = 0;

testRoom = {
    "name": "Sala de Teste",
    "id": "a1b2c3",
    "users": [],
    "maxUsers" : 2,
    "messages": [],
    "passwordProtectd": false
}

testUser = {
    "nickname" : "John Doe",
}

let rooms = [];
let users = [];

//Test endpoint
server.get('/teste', (req, res) => {
    res.send(JSON.stringify({'payload': 'Teste!'}));
})

//Joing a room
server.post('/api/rooms/join/:roomId', (req, res) => {
    console.log(timestamp() + "Client at " + req.ip + " tried to join room " + req.params.roomId);
    let opCode = addUserToRoom(req.body.nickname, req.params.roomId);
    let status, message;
    switch (opCode){
        //Server is full
        case 0:
            status = 503;
            message = "Server is full";
            console.log(timestamp() + req.ip + " couldn't join room " + req.params.roomId, ": server is full."); 
            break;
        //Added to room
        case 1:
            status = 200; //OK
            message = "Added to room.";
            console.log(timestamp() +req.ip + " joined room " + req.params.roomId); 
            break;
        //Room is full
        case 2:
            status = 503;
            message = "Room is full.";
            console.log(timestamp() +req.ip + " couldn't join room " + req.params.roomId +": room is full."); 
            break;
        //Room does not exist
        case 3:
            status = 404;
            message = "Room does not exist";
            console.log(timestamp() +req.ip + " couldn't join room " + req.params.roomId + ": room does not exist."); 
            break;
        default:
            status = 500;
            message = "Internal server error. Contact system administrator."
            console.log(timestamp() +req.ip + " couldn't join room " + req.params.roomId +": some obscure error has ocurred."); 
            break;
    }
    res.status(status);
    res.send({"message":message});

});


//Leaving a room
server.post('/api/rooms/leave/:roomId', (req, res) => {
    console.log(timestamp() + "Client at " + req.ip + " tried to leave room " + req.params.roomId);
    let opCode = removeUserFromRoom(req.body.user, req.params.roomId);
    let status, message;
    switch(opCode){
        case 1:
            status = 200;
            message = "Left room"
            console.log(timestamp() + req.ip + "left room " + req.params.roomId);
            break;
        case 2:
            status = 404;
            message = "Room does not exist";
            console.log(timestamp()  + req.ip + " couldn't leave room " + req.params.roomId + ": room does not exist."); 
            break;
        case 3:
            status = 401;
            message = "User not in this room";
            console.log(timestamp() +req.ip + " couldn't leave room " + req.params.roomId + ": user not in this room."); 
            break;
        default:
            status = 500;
            message = "Internal server error. Contact system administrator."
            console.log(timestamp() +req.ip + " couldn't post to room " + req.params.roomId +": some obscure error has ocurred."); 
            break;
    }
    postLeaveMessage(req.body.user, req.params.roomId);
    res.status(status);
    res.send({"message" : message});
})

//Creating rooms
server.post('/api/rooms/create', (req, res) => {
    console.log(timestamp() + "Client at " + req.ip + " trying to create room ");
    let opCode = createRoom(req.body);
    let status, message;
    switch(opCode){
        case 0:
            status = 200;
            message = "Created room."
            console.log(timestamp() + req.ip + " createad a room");
            break;
        case 1:
            status = 401;
            message = "Max number of rooms reached."
            console.log(timestamp() + req.ip + " failed to create a room - max number of rooms reached");
            break;
        default:
            status = 500;
            message = "Internal server error. Contact system administrator."
            console.log(timestamp() +req.ip + " couldn't post to room " + req.params.roomId +": some obscure error has ocurred."); 
            break;
    }
    res.status(status);
    res.send({"message" : message});
})


//Requesting the room list
server.get('/api/rooms/list', (req, res) => {
    console.log(timestamp() + 'Sending room list to client at ' + req.ip)
    res.send(JSON.stringify(rooms));
})

//Sending messages to a room.
server.post('/api/message/send/:roomId', (req, res) => {
    console.log(timestamp() + "User at " + req.ip + " tried to post to room " + req.params.roomId);

    //Checks to see if the user is in the room it is trying to send the message
    let opCode = postMessageToRoomCheck(req.body.user, req.params.roomId);
    let status, message;
    switch(opCode){
        case 0:
            status = 200;
            message = "Posted to room"
            console.log(timestamp() + req.ip + " posted to room " + req.params.roomId);
            break;
        case 1:
            status = 401;
            message = "User not in this room";
            console.log(timestamp() +req.ip + " couldn't post to room " + req.params.roomId + ": user not in this room."); 
            break;
        case 2:
            status = 404;
            message = "Room does not exist";
            console.log(timestamp() +req.ip + " couldn't post to room " + req.params.roomId + ": room does not exist."); 
            break;
        default:
            status = 500;
            message = "Internal server error. Contact system administrator."
            console.log(timestamp() +req.ip + " couldn't post to room " + req.params.roomId +": some obscure error has ocurred."); 
            break;
    }
    if(status == 200){
        postMessageToRoom(req.body.user, req.params.roomId, req.body.message);
    }
    res.status(status);
    res.send({"message" : message});

});

/*
On the client side, a thread keeps requesting to the server the most current message.
If the message it sends is diferent from the most current one, it sends it back to the client.
*/
server.post('/api/message/receive/:roomId', (req, res) => {
    //console.log(timestamp() + "Client at " + req.ip + " trying to read messages at room " + req.params.roomId);
    const room = rooms.find(r => r.id === req.params.roomId);
    if(room){
        if(room.messages && room.messages.length > 0){
            const lastRoomMessage = room.messages[room.messages.length - 1];
            const lastClientMessage = JSON.parse(req.body.lastMessage);
            
            if(!compareMessages(lastClientMessage,lastRoomMessage)){
                res.status(200);
                res.send(lastRoomMessage);
            }else{
                res.status(401);
                res.send();
            }
        }else{
            res.status(401);
            res.send();
        }
    }else{
        console.log(timestamp() + "Client " + res.ip + " failed to read " + req.params.roomId + ": room does not exists");
        res.status(404);
        res.send({"message": "Couldn't send message: room does not exist"});
    }

})





server.listen(PORT, () => {
    console.log(timestamp() + "Server listening on port " + PORT);
})




function addUserToRoom(nickname, roomId){
    let message = "";
    if(currentUsers >= MAX_USERS){
        return 0;
    }
    for (room in rooms){
        if(rooms[room].id == roomId){
            if(rooms[room].users.length < rooms[room].maxUsers){
                currentUsers += 1;
                rooms[room].users.push({"nickname":nickname});
                return 1;
            }else{
                return 2;
            }
        }
    }
    return 3;
}

function removeUserFromRoom(userLeaving, roomId){
    const room = rooms.find(r => r.id === roomId);
    if(room){
        const user = room.users.find(u => compareUsers(u, userLeaving));
        if(user){
            room.users = room.users.filter(u => u != user);
            console.log(room.currentUsers);
            if(room.currentUsers > 0){
                room.currentUsers = room.currentUsers - 1;
                
            }
            if(currentUsers > 0){
                currentUsers = currentUsers - 1;
            }
            return 1;
        }else{
            return 2;
        }
    }else{
        return 3;
    }
}

//Checks to see if the user is in the specified room.
function postMessageToRoomCheck(user, roomId){
    for(room in rooms){
        if(rooms[room].id == roomId){
            for(_user in rooms[room].users){
                if(user.nickname == rooms[room].users[_user].nickname){
                    return 0;
                }
            }
            return 1;
        }
    }
    return 2;
}

function postMessageToRoom(user, roomId, message){
    let room;
    for(_room in rooms){
        if(rooms[_room].id == roomId){
            room = rooms[_room];
        }
    }
    let ts = timestamp();
    let clientTs = clientTimestamp();
    let messageJson = {
        'content' : message,
        'user': user.nickname,
        'clientTs': clientTs,
        'ts': ts
    }
    console.log(messageJson);
    room.messages.push(messageJson);
    for(_room in rooms){
        if(rooms[_room].id == roomId){
            rooms[_room] = room;
        }
    }
}

//The message the server sends to the room when a user leaves it.
function postLeaveMessage(user, roomId){
    const room = rooms.find(r => r.id === roomId);
    if(room){
        let ts = timestamp();
        let clientTs = clientTimestamp();
        let leaveString = user.nickname + ' has left the room.';
        let leaveMessageJson = {
            'content' : leaveString,
            'user': 'server',
            'clientTs' : clientTs,
            'ts' : ts
        }
        room.messages.push(leaveMessageJson);
        for(_room in rooms){
            if(rooms[_room].id == roomId){
                rooms[_room] = room;
            }
        }
    }
}

function createRoom(roomToCreate){
    if(rooms.length >= MAX_ROOMS){
        return 1;
    }else{
        let roomId = getRanHex(6);
        let newRoom = {
            "name" : roomToCreate.name,
            "id" : roomId,
            'users' : [],
            'maxUsers' : roomToCreate.maxUsers,
            'messages' : []
        }
        rooms.push(newRoom);
        return 0;
    }
}


/* 
Used for generating roomIds.
TODO: Add UserIds.
*/
const getRanHex = size => {
    let result = [];
    let hexRef = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'];
  
    for (let n = 0; n < size; n++) {
      result.push(hexRef[Math.floor(Math.random() * 16)]);
    }
    return result.join('');
  }

function timestamp(){
    let now = new Date(Date.now());
    return("[" + now.getHours() + ":" + now.getMinutes() + ":" + now.getMilliseconds() + "]");
}

//A more human friendly timestamp. Used for client side rendering.
function clientTimestamp(){
    let now = new Date(Date.now());
    let hours = now.getHours();
    let minutes = now.getMinutes();
    if(hours < 10){
        hours = '0' + hours;
    }
    if(minutes < 10){
        minutes = '0' + minutes;
    }
    return("[" + hours + ":" + minutes + "]");
}
  
function compareMessages(message1, message2){
    if(
        (message1) &&
        (message2) &&
        (message1.content == message2.content) &&
        (message1.user == message2.user) &&
        (message1.ts == message2.ts)
    ){
        return true;
    }else{
        return false;
    }
}

function compareUsers(user1, user2){
    if(
        (user1.nickname == user2.nickname)
    ){
        return true;
    }else{
        return false;
    }
}