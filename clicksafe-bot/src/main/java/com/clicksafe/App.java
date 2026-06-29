package com.example;

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

        if(messageContent.length() > 0)
            if(!messageContent.substring(0,1).equals(","))
                return;

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
            default:
                return "Command received!";
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
    public void sendChannel(String id, String mess){
        System.out.println("\n***CHANNEL MESSAGE***");
        TextChannel textChannel = jda.getTextChannelById(id);

        if (textChannel.canTalk()) {
            textChannel.sendMessage("@everyone" + "\n" + mess).queue();
            System.out.println("SENT: '" + mess + "' \nTO: " + textChannel.getName() + "-" + id + "\n");
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
        Scanner parse = new Scanner(line);
        String id = parse.next();
        String user = parse.next();
        String mess = parse.nextLine();

        System.out.println("\nProcessing: '" + mess + "' on: " + Thread.currentThread().getName());

        try{sendUser(id, user, commandHandler(id, user, mess));}catch(InterruptedException e){System.out.println("CAUGHT EXCEPTION");}

    }

    public void receiveMessage(String line) {
        messageQueue.offer(line);
    }
}












