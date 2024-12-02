/*
 * Glassbottle Client.
 * Created by Lucas Moura, 2024.
 * 
 * The client made to interact with the corresponding Express Server.
 * Communication all done using REST. Improvements still needed. 
 *
 */

package edu.glassbottle.lucaslpmoura;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Scanner;

public class Client {

    // SERVER ADDRESS
    private static String serverPort = "5000";
    private static String serverAddress = "http://localHost:5000"; // default server address

    // ENDPOINTS
    private static String testEndpoint = "/teste";

    private static String listRoomsEndpoint = "/api/rooms/list";
    private static String createRoomEndpoint = "/api/rooms/create";
    private static String joinRoomEndpoint = "/api/rooms/join/";
    private static String leaveRoomEndpoint = "/api/rooms/leave/";
    private static String sendMessageEndpoint = "/api/message/send/";
    private static String receiveMessageEndpoint = "/api/message/receive/";

    // HTTP CLIENT
    private static HttpClient client = HttpClient.newHttpClient();

    // USER VARIABLES
    private static String nickname;
    private static String currentRoomId = "";
    private static JSONObject lastMessage = new JSONObject();

    // SYSTEM VARIABLES
    private static Scanner sc = new Scanner(System.in);

    public static enum State {
        LOGIN,
        MAIN_MENU,
        CREATE_ROOM,
        ROOM
    }

    private static State state = State.LOGIN;

    public static enum OS {
        WINDOWS,
        MAC,
        LINUX
    }

    private static OS detectedOS;

    public static void main(String args[]) throws IOException, InterruptedException {
        detectedOS = detectOS();

        for (;;) {
            switch (state) {
                case LOGIN:
                    login();
                    break;
                case MAIN_MENU:
                    mainMenu();
                    break;
                case ROOM:
                    parseRoom();
                    break;
                default:
                    break;
            }
        }

        // sc.close();
    }

    // Gets the username and sends it to the server
    public static void login() throws IOException, InterruptedException {
        System.out.print("Enter server address (leave empty for default): ");
        String serverAddr = sc.nextLine();
        if (!serverAddr.equals("")) {
            serverAddress = "http://" + serverAddr + ":" + serverPort;
            ;
        }
        System.out.print("Enter your nickname: ");
        nickname = sc.nextLine();
        state = State.MAIN_MENU;
        clearScreen();
    }

    // Prints the main menu and handles basic joining/creating rooms

    public static void mainMenu() throws IOException, InterruptedException {

        state = State.MAIN_MENU;

        System.out.println("You are connected as: " + nickname);
        System.out.println("Welcome to Glassbottle.");

        JSONArray roomList = listRooms();

        while (true) {
            String option = sc.next();

            // If the option is a number, try to join the corresponding room
            if (isNumeric(option)) {
                int roomCode = Integer.parseInt(option);
                if ((roomCode > 0) && (roomCode <= roomList.length())) {
                    try {
                        joinRoom(roomList.getJSONObject(roomCode - 1).getString("id"));
                        return;
                    } catch (Exception e) {
                        clearScreen();
                        System.out.println("Couldn't join room: " + e.getMessage());
                    }
                } else {
                    clearScreen();
                    roomList = listRooms();
                    System.out.println("Please enter an valid option or command.");
                }

                // If it is a letter, executs the command
            } else {
                if (option.equals("c")) {
                    clearScreen();
                    try {
                        createRoom();
                        roomList = listRooms();
                        System.out.println("Created room.");
                    } catch (Exception e) {
                        roomList = listRooms();
                        System.out.println(e.getMessage());
                    }
                } else {
                    if (option.equals("r")) {
                        clearScreen();
                        roomList = listRooms();
                        System.out.println("Room List refreshed.");
                    } else {
                        if (option.equals("q")) {
                            System.out.println("Goodbye! ");
                            System.exit(0);
                        } else {
                            clearScreen();
                            roomList = listRooms();
                            System.out.println("Please enter an valid option or command.");
                        }
                    }
                }

            }
        }
    }

    // Handles creating the request to create a room

    public static void createRoom() throws IOException, InterruptedException, Exception {
        System.out.print("Room name: ");

        sc.nextLine(); // The input needs to be flushed here, god knows why

        String roomName = sc.nextLine();
        System.out.print("Max Users: ");
        int maxUsers = sc.nextInt();

        JSONObject room = new JSONObject();
        room.put("name", roomName);
        room.put("maxUsers", maxUsers);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverAddress + createRoomEndpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(BodyPublishers.ofString(room.toString()))
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        clearScreen();

        // Failing to create a room
        if (response.statusCode() != 200) {
            throw new Exception(new JSONObject(response.body()).getString("message"));
        }

    }

    // Requst to the server the list of rooms and builds it

    public static JSONArray listRooms() throws IOException, InterruptedException {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverAddress + listRoomsEndpoint)).build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            JSONArray roomList = new JSONArray(response.body());

            boolean avaiableRooms = true;
            if (roomList.isEmpty()) {
                System.out.println("No rooms avaiable.");
                avaiableRooms = false;
            }

            if (avaiableRooms) {
                System.out.println(
                        "Press the room number to join. Or press 'c' to  create a new room, and 'r' to refresh the list. Press 'q' to exit the program.");
                for (int i = 0; i < roomList.length(); i++) {
                    System.out.print((i + 1) + ") ");
                    System.out.println(roomList.getJSONObject(i).getString("name"));
                }

                // No rooms avaiable
            } else {
                System.out.println(
                        "Be the first to create a room by pressing 'c'. Press 'r' to refresh the list. Press 'q' to exit the program.");
            }

            return roomList;
        } catch (Exception e) {
            System.out.println("Couldn't connect to server. Please try again.");
            System.exit(1);
            return null;
        }

    }

    // Tries to join an existing room

    public static void joinRoom(String roomId) throws IOException, InterruptedException {
        clearScreen();
        JSONObject user = new JSONObject();
        user.put("nickname", nickname);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverAddress + joinRoomEndpoint + roomId))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(user.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            state = State.ROOM;
            System.out.println("Joined room. Press 'q' to return to the main menu.");
            currentRoomId = roomId;
        } else {
            System.out.print("Couldn't join room: " + response.statusCode() + " - ");
            JSONObject resp = new JSONObject(response.body());
            System.out.println(resp.getString("message"));
            state = State.MAIN_MENU;
        }
    }

    // Leaves the current room

    public static void leaveRoom() throws IOException, InterruptedException {
        JSONObject jsonMessage = new JSONObject();
        JSONObject jsonNickname = new JSONObject();
        jsonNickname.put("nickname", nickname);
        jsonMessage.put("user", jsonNickname);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverAddress + leaveRoomEndpoint + currentRoomId))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(jsonMessage.toString()))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Error leaving room: " + e.getMessage());
        }

    }

    /*
     * The most complex function of the code.
     * When joining a room, creates 2 threads: one for writing, and one for reading.
     * 
     * READ
     * The read thread keeps making requsts to the server each 800 ms, sendind the
     * last message it recieved.
     * If it matches the most current message on the server side, it simply does
     * nothing.
     * Else, it prints the last message.
     * 
     * WRITE
     * Reads the input from the user and creates the JSON object for the server to
     * parse the message.
     * After that, it deletes the current line.
     * 
     * Doing it that way prevents the 2 operations from blocking one another, but I
     * admit that this code is not the most elegant.
     */

    public static void parseRoom() throws IOException, InterruptedException {
        boolean isReceiveThreadRunning = false;
        Thread recieveThread = new Thread() {
            public void run() {
                while (currentRoomId != "") {
                    try {
                        JSONObject lastMessageJSON = new JSONObject();
                        lastMessageJSON.put("lastMessage", lastMessage.toString());

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(serverAddress + receiveMessageEndpoint + currentRoomId))
                                .header("Content-Type", "application/json")
                                .timeout(Duration.ofSeconds(3))
                                .POST(BodyPublishers.ofString(lastMessageJSON.toString()))
                                .build();
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 200) {
                            lastMessage = new JSONObject(response.body());
                            if (!lastMessage.get("content").equals("")) {
                                if (lastMessage.get("user").equals("server")) {
                                    printServerMessage(lastMessage);
                                } else {
                                    printUserMessage(lastMessage);
                                }

                            }
                        }
                        Thread.sleep(500);
                    } catch (Exception e) {
                        System.out.println("Error reading message from server. ");
                        if (e.getMessage() != null) {
                            System.out.println(e.getMessage());
                        }
                        setClientState("MAIN_MENU");
                        return;
                    }
                }

            }
        };

        boolean firstTime = true;
        while (currentRoomId != "") {
            // System.out.print("Message: ");
            if (!isReceiveThreadRunning) {
                isReceiveThreadRunning = true;
                recieveThread.start();
            }
            String message = sc.nextLine();

            // Leaving the room
            if (message.equals("q")) {
                leaveRoom();
                state = State.MAIN_MENU;
                currentRoomId = "";
                break;
            }

            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("message", message);

            JSONObject jsonNickname = new JSONObject();
            jsonNickname.put("nickname", nickname);
            jsonMessage.put("user", jsonNickname);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(serverAddress + sendMessageEndpoint + currentRoomId))
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString(jsonMessage.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    System.out.print("Couldn't send message: ");
                    JSONObject resp = new JSONObject(response.body());
                    System.out.println(resp.getString("message"));
                }

                // Just so the "Joined room." message doesnt gets deleted
                if (firstTime) {
                    firstTime = false;
                } else {
                    // Move cursor up and delete the bottom line
                    System.out.print(String.format("\033[%dA", 1)); // Move up
                    System.out.print("\033[2K"); // Erase line content
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        isReceiveThreadRunning = false;
        clearScreen();
        return;
    }

    private static void printUserMessage(JSONObject message) {
        System.out.print(message.get("clientTs") + " ");
        System.out.print(message.get("user") + ": ");
        System.out.println(message.get("content"));
    }

    // If the server sends a message, no user is displayed
    private static void printServerMessage(JSONObject message) {
        System.out.print(message.get("clientTs") + " ");
        System.out.println(message.get("content"));
    }

    // Needed so that the read thread can alter the state of the state machine
    public static void setClientState(String newState) {
        if (newState.equals("MAIN_MENU")) {
            state = State.MAIN_MENU;
        }
    }

    public static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Why Microsoft... "clear" was fine
    private static OS detectOS() {
        String currentOS = System.getProperty("os.name");
        if (currentOS.startsWith("Windows")) {
            return OS.WINDOWS;
        }
        if (currentOS.startsWith("Linux")) {
            return OS.LINUX;
        }
        if (currentOS.startsWith("Ubuntu")) {
            return OS.LINUX;
        }
        if (currentOS.startsWith("Debian")) {
            return OS.LINUX;
        }
        if (currentOS.startsWith("Mac")) {
            return OS.MAC;
        }
        if (currentOS.startsWith("Darwin")) {
            return OS.MAC;
        }
        return null;
    }

    private static void clearScreen() {
        try {
            if (detectedOS.equals(OS.WINDOWS)) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033\143");
            }
        } catch (Exception e) {
            System.out.println("Error clearing screen: " + e.getMessage());
        }

    }
}