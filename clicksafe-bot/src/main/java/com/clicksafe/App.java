package com.clicksafe;

import com.fazecast.jSerialComm.SerialPort;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.InputStream;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


public class App extends ListenerAdapter {

    private static JDA jda;

    //stuff for queue threads
    private final LinkedBlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    public static App listener = new App();

    //misc vars
    public volatile String mainReturnID = "";

    public static void main(String[] args) throws InterruptedException {

        String botToken = System.getenv("DISCORD_BOT_TOKEN");
        if (botToken == null || botToken.isEmpty()) {
            System.err.println("Bot token not found. Set DISCORD_BOT_TOKEN environment variable.");
            return;
        }

        jda = JDABuilder.createDefault(botToken, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
        .addEventListeners(listener)
        .build();
        jda.awaitReady();
        listener.startListening();

        System.out.println("*****ONLINE*****");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        if (event.getAuthor().isBot()) return;
        String messageContent = event.getMessage().getContentRaw();
        String authorName     = event.getAuthor().getName();
        String channelName    = event.getChannel().getName();
        String id             = event.getChannel().getId();

        if(messageContent.length() > 0){
            if(!messageContent.substring(0,1).equals(","))
                return;
        }
        else return;

        System.out.println("\nCHANNEL: " + channelName + "-" + id + "\nUSER: " + authorName + "\nMESS: " + messageContent);
        listener.receiveMessage(id + " " + authorName + " " + messageContent.substring(1));
    }

    //////////////////////////////////////////
    //COMMAND HANDLING////////////////////////
    //////////////////////////////////////////



    public static String commandHandler(String channelid, String user, String line)throws InterruptedException{
        Scanner input = new Scanner(line);
        String first = "";
        if(line.length() > 0)
            first = input.next();

        switch(first){
            case "start":
                listener.receiveMessage("239845");
                return "Okay...";
            default:
                return "I'm sorry. I did not understand this message.";
        }

    }

    //////////////////////////////////////////
    //HELPER METHODS//////////////////////////
    //////////////////////////////////////////

    public static void delay(int t){
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void usbListener(){
        delay(500);
        System.out.println("Attempting USB listener...");
        //COM3, /dev/ttyUSB0, /dev/ttyACM0
        String portName = "/dev/ttyUSB0";
        int baudRate = 9600;
        SerialPort port;
        System.out.println("PORT: " + portName + "\nBR: " + baudRate);
        try{
            port = SerialPort.getCommPort(portName);
        }catch(Throwable t){
            System.out.println("Exception occurred");
            t.printStackTrace();
            sendChannel("I don't think the receiver is plugged in :/");
            return;
        }
        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        System.out.println("Trying port...");
        if(!port.openPort()){
            System.err.println("Failed to open port: " + portName);
            sendChannel("Sorry yall, I couldn't find a signal :(");
            return;
        }
        System.out.println("Port opened. Listening...");
        sendChannel("Good news! I found a signal :)");
        try(InputStream in = port.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in))){

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("RX: " + line);
                sendChannel(line);
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            port.closePort();
            System.out.println("Port closed.");
            sendChannel("I closed my signal :( no one was talking to me.");
        }
    }

    //////////////////////////////////////////
    //THREAD AND FINAL HANDLING///////////////
    //////////////////////////////////////////

    public void sendUser(String id, String user, String mess){
        System.out.println("\n***USER MESSAGE***");
        TextChannel textChannel = jda.getTextChannelById(id);

        if (textChannel.canTalk()) {
            textChannel.sendMessage("@" + user + "\n" + mess).queue();
            System.out.println("SENT: '" + mess + "' \nTO: " + textChannel.getName() + "-" + id + "\n");
        }
    }
    public void sendChannel(String mess){
        System.out.println("\n***CHANNEL MESSAGE***");
        TextChannel textChannel = jda.getTextChannelById(mainReturnID);

        if (textChannel.canTalk()) {
            textChannel.sendMessage("@everyone" + "\n" + mess).queue();
            System.out.println("SENT: '" + mess + "' \nTO: " + textChannel.getName() + "-" + mainReturnID + "\n");
        }
    }
    public void startListening(){
        Thread listenerThread = new Thread(() -> {
            while (true) {
                try {
                    String line = messageQueue.take(); // Blocks until a message arrives

                    executor.submit(() -> {
                        try {
                            handleMessage(line); // This may throw IOException
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        listenerThread.start();
    }

    private void handleMessage(String line)throws IOException{
        if(line.equals("239845"))
            usbListener();
        else{
            Scanner parse = new Scanner(line);
            String id = parse.next();
            String user = parse.next();
            String mess = parse.nextLine();

            mainReturnID = id;

            System.out.println("\nProcessing: '" + mess + "' on: " + Thread.currentThread().getName());

            try{sendUser(id, user, commandHandler(id, user, mess));}catch(InterruptedException e){System.out.println("CAUGHT EXCEPTION");}
        }
    }

    public void receiveMessage(String line) {
        messageQueue.offer(line);
    }
}












