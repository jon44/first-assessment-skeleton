package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	private String username;
	private HashMap<String, Socket> clientMap;

	public ClientHandler(Socket socket, HashMap<String, Socket> clientMap) {
		super();
		this.socket = socket;
		this.clientMap = clientMap;
	}
	
	public void toAll(String response) {
		Collection<Socket> allSocks = clientMap.values();
		PrintWriter writer;

		try {
			for(Socket s : allSocks) {
				writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
				writer.write(response);
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void toOne(String response, Socket socket) {
		PrintWriter writer;
		
		try {
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			writer.write(response);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String response;
			String command;
			String otherUser = "";
			String users = "";
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");

			while (!socket.isClosed()) {				
				Set<String> allUsers;
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				message.setTimestamp(LocalDateTime.now().format(formatter));
				
				command = message.getCommand();
				if(command.startsWith("@")) {
					otherUser = command.substring(1, command.length());
					command = "@";
				}
				
				switch (command) {
					case "connect":
						if(clientMap.containsKey(message.getUsername())) {
							message.setCommand("fail");
							message.setContents("username \"" + message.getUsername() + "\" is already taken");
							response = mapper.writeValueAsString(message);
							this.toOne(response, this.socket);
							this.socket.close();
						} else {
							log.info("user <{}> connected", message.getUsername());
							username = message.getUsername();
							response = mapper.writeValueAsString(message);
							this.toAll(response);
							clientMap.put(message.getUsername(), socket);
						}
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						response = mapper.writeValueAsString(message);
						clientMap.remove(message.getUsername());
						this.toAll(response);
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						response = mapper.writeValueAsString(message);
						this.toOne(response, this.socket);
						break;
					case "broadcast":
						log.info("user <{}> broadcast message <{}>", message.getUsername(), message.getContents());
						response = mapper.writeValueAsString(message);
						this.toAll(response);
						break;
					case "@":
						log.info("user <{}> messaged <{}> to user <{}>", message.getUsername(), message.getContents(), otherUser);
						message.setCommand("@");
						if(clientMap.containsKey(otherUser)) {
							response = mapper.writeValueAsString(message);
							this.toOne(response, clientMap.get(otherUser));
						} else {
							message.setCommand("!@");
							message.setContents("this user does not exist");
							response = mapper.writeValueAsString(message);
							this.toOne(response, this.socket);
						}
						break;
					case "users":
						log.info("user <{}> requested users", message.getUsername());
						allUsers = clientMap.keySet();
						users = "";
						for(String i : allUsers) {
							if(users.equals("")) {
								users = i;
							} else {
								users = i + "\n" + users;
							}
						}
						message.setContents(users);
						response = mapper.writeValueAsString(message);
						this.toOne(response, this.socket);
						break;
				}
			}

		} catch (IOException e) {
			clientMap.remove(this.username);
			Message message = new Message();
			message.setCommand("disconnect");
			message.setUsername(this.username);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
			message.setTimestamp(LocalDateTime.now().format(formatter));
			ObjectMapper mapper = new ObjectMapper();
			
			try {
				String response = mapper.writeValueAsString(message);
				this.toAll(response);
				
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
			
			log.error("Something went wrong :/", e);
		}
	}

}
