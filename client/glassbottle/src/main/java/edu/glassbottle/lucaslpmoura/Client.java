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
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

public class Client {
    private static String serverAddress = "http://localhost:5000";
    // private static String serverAddress = "http://172.28.193.218:5000";

    // ENDPOINTS
    private static String testEndpoint = "/teste";

    private static String loginEndpoint = "/api/login";
    private static String listRoomsEndpoint = "/api/rooms/list";
    private static String createRoomEndpoint = "/api/rooms/create";
    private static String joinRoomEndpoint = "/api/rooms/join/";
    private static String leaveRoomEndpoint = "/api/room/leave/";
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

    private static enum State {
        LOGIN,
        MAIN_MENU,
        CREATE_ROOM,
        ROOM
    }

    private static State state = State.LOGIN;

    public static void main(String args[]) throws IOException, InterruptedException {
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
        System.out.print("Enter your nickname: ");
        nickname = sc.nextLine();
        state = State.MAIN_MENU;
        // new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); //
        // TODO: Add Linux Support
    }

    // Prints the main menu and handles basic joining/creating rooms

    public static void mainMenu() throws IOException, InterruptedException {

        state = State.MAIN_MENU;

        System.out.println("You are connected as: " + nickname);

        JSONArray roomList = listRooms();

        while (true) {
            String option = sc.next();
            if (isNumeric(option)) {
                int roomCode = Integer.parseInt(option);
                if ((roomCode > 0) && (roomCode <= roomList.length())) {
                    try {
                        joinRoom(roomList.getJSONObject(roomCode - 1).getString("id"));
                        return;
                    } catch (Exception e) {
                        // new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); //
                        // TODO: Add Linux Support
                        System.out.println("Couldn't join room: " + e.getMessage());
                    }
                } else {
                    // new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); //
                    // TODO: Add Linux Support
                    roomList = listRooms();
                    System.out.println("Please enter an valid option or command.");
                }
            } else {
                if (option.equals("c")) {
                    createRoom();
                    roomList = listRooms();
                }
                if (option.equals("r")) {
                    // new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); //
                    // TODO: Add Linux Support
                    roomList = listRooms();
                    System.out.println("Room List refreshed.");
                } else {
                    // new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); //
                    // TODO: Add Linux Support
                    roomList = listRooms();
                    System.out.println("Please enter an valid option or command.");
                }
            }
        }
    }

    public static void createRoom() throws IOException, InterruptedException {
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
                .POST(BodyPublishers.ofString(room.toString()))
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        // new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); //
        // TODO: Add Linux Supoort
        System.out.println(response.body().toString());
 
    }

    public static JSONArray listRooms() throws IOException, InterruptedException {
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
                    "Press the room number to join. Or press 'c' to  create a new room, and 'r' to refresh the list.");
            for (int i = 0; i < roomList.length(); i++) {
                System.out.print((i + 1) + ") ");
                System.out.println(roomList.getJSONObject(i).getString("name"));
            }
            System.out.print("\n");
        } else {
            System.out.print("Be the first to create a room by pressing 'c'. Press 'r' to refresh. the list");
        }

        return roomList;

    }

    public static void joinRoom(String roomId) throws IOException, InterruptedException {
        // new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); //
        // TODO: Add Linux Support

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
                                .POST(BodyPublishers.ofString(lastMessageJSON.toString()))
                                .build();
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 200) {
                            lastMessage = new JSONObject(response.body());
                            if (!lastMessage.get("content").equals("")) {
                                System.out.print(lastMessage.get("clientTs") + " ");
                                System.out.print(lastMessage.get("user") + ": ");
                                System.out.println(lastMessage.get("content"));
                            }
                        }
                        Thread.sleep(500);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
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
        // new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); //
        // TODO: Add Linux Support
        return;
    }

    public static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}