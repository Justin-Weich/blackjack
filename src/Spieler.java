import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Spieler {
    private String name;
    private int port;
    private String ipAddress;
    private DatagramSocket socket;
    private String croupierAddress;
    private int croupierPort;
    private ObjectMapper objectMapper;
    private ArrayList<Card> hand;

    public Spieler(String name, String ipAddress, int port) throws SocketException {
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
        this.socket = new DatagramSocket(port);
        this.objectMapper = new ObjectMapper();
        this.hand = new ArrayList<>();
    }

    public void register(String croupierAddress, int croupierPort) throws IOException {
        this.croupierAddress = croupierAddress;
        this.croupierPort = croupierPort;
        String message = "registerPlayer " + ipAddress + " " + port + " " + name;
        sendMessage(message);
    }

    public void placeBet(int betAmount) throws IOException {
        if (croupierAddress == null || croupierPort == 0) {
            System.out.println("You must register first.");
            return;
        }
        String message = "bet " + name + " " + betAmount;
        sendMessage(message);
    }

    public void hit() throws IOException {
        Card lastCard = hand.get(hand.size() - 1);
        String message = "hit " + name + " " + lastCard.getDeck() + " " + lastCard.toString();
        sendMessage(message);
    }

    public void stand() throws IOException {
        Card lastCard = hand.get(hand.size() - 1);
        String message = "stand " + name + " " + lastCard.getDeck() + " " + lastCard.toString();
        sendMessage(message);
    }

    public void split() throws IOException {
        Card lastCard = hand.get(hand.size() - 1);
        String message = "split " + name + " " + lastCard.getDeck() + " " + lastCard.toString();
        sendMessage(message);
    }

    public void doubleDown() throws IOException {
        Card lastCard = hand.get(hand.size() - 1);
        String message = "doubleDown " + name + " " + lastCard.getDeck() + " " + lastCard.toString();
        sendMessage(message);
    }

    public void surrender() throws IOException {
        Card lastCard = hand.get(hand.size() - 1);
        String message = "surrender " + name + " " + lastCard.getDeck() + " " + lastCard.toString();
        sendMessage(message);
    }

    private void confirmCardReceived(Card card) throws IOException {
        String message = "player " + name + " received " + card.getDeck() + " " + card.toString();
        sendMessage(message);
    }

    private void acceptPrize(int amount) throws IOException {
        String message = "prize accepted " + name;
        System.out.println("Prize received: " + amount);
        sendMessage(message);
    }

    private void handleGameOver(String message) throws IOException {
        System.out.println("Game over: " + message);
        String response = "gameover " + name;
        sendMessage(response);
    }

    private void sendMessage(String message) throws IOException {
        byte[] buffer = message.getBytes();
        InetAddress address = InetAddress.getByName(this.croupierAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, this.croupierPort);
        socket.send(packet);

        buffer = new byte[1024];
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        String response = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Received: " + response);

        if (response.startsWith("{")) {
            // Assuming response is a Card JSON object
            try {
                Card card = objectMapper.readValue(response, Card.class);
                System.out.println("Received card: " + card);
                hand.add(card);
                confirmCardReceived(card);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        } else {
            if (response.equals("bet accepted") || response.equals("action accepted")) {
                System.out.println("Action was accepted.");
            } else if (response.startsWith("bet declined") || response.startsWith("action declined")) {
                System.out.println("Action was declined: " + response.substring(response.indexOf(' ') + 1));
            } else if (response.equals("registration successful")) {
                System.out.println("Registration was successful.");
            } else if (response.startsWith("registration declined")) {
                System.out.println("Registration was declined: " + response.substring("registration declined".length()));
            } else if (response.startsWith("prize")) {
                int amount = Integer.parseInt(response.split(" ")[1]);
                acceptPrize(amount);
            } else if (response.startsWith("gameover")) {
                handleGameOver(response.substring("gameover".length()).trim());
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java Spieler <PlayerIP> <PlayerPort> <Name>");
            return;
        }

        String ipAddress = args[0];
        int port = Integer.parseInt(args[1]);
        String name = args[2];

        try {
            Spieler player = new Spieler(name, ipAddress, port);
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("Enter command:");
                String command = scanner.nextLine();

                if (command.startsWith("registerPlayer")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 4) {
                        String croupierAddress = parts[1];
                        int croupierPort = Integer.parseInt(parts[2]);
                        player.register(croupierAddress, croupierPort);
                    } else {
                        System.out.println("Invalid command. Usage: registerPlayer <CroupierIP> <CroupierPort> <Name>");
                    }
                } else if (command.startsWith("bet")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 3) {
                        int betAmount = Integer.parseInt(parts[2]);
                        player.placeBet(betAmount);
                    } else {
                        System.out.println("Invalid command. Usage: bet <Name> <Amount>");
                    }
                } else if (command.startsWith("hit")) {
                    player.hit();
                } else if (command.startsWith("stand")) {
                    player.stand();
                } else if (command.startsWith("split")) {
                    player.split();
                } else if (command.startsWith("doubleDown")) {
                    player.doubleDown();
                } else if (command.startsWith("surrender")) {
                    player.surrender();
                } else {
                    System.out.println("Unknown command.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
