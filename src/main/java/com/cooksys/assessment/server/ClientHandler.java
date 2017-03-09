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
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	private HashMap<String, Socket> clientMap;

	public ClientHandler(Socket socket, HashMap<String, Socket> clientMap) {
		super();
		this.socket = socket;
		this.clientMap = clientMap;
	}

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter tempWriter;
			String response;
			String command;
			String otherUser = "";
			String users = "";
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

			while (!socket.isClosed()) {
				Collection<Socket> allSocks;
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
							tempWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
							tempWriter.write(response);
							tempWriter.flush();
							this.socket.close();
						} else {
							log.info("user <{}> connected", message.getUsername());
							response = mapper.writeValueAsString(message);
							allSocks = clientMap.values();
							for(Socket i : allSocks) {
								tempWriter = new PrintWriter(new OutputStreamWriter(i.getOutputStream()));
								tempWriter.write(response);
								tempWriter.flush();
							}
							clientMap.put(message.getUsername(), socket);
						}
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						response = mapper.writeValueAsString(message);
						clientMap.remove(message.getUsername());
						allSocks = clientMap.values();
						for(Socket i : allSocks) {
							tempWriter = new PrintWriter(new OutputStreamWriter(i.getOutputStream()));
							tempWriter.write(response);
							tempWriter.flush();
						}
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						response = mapper.writeValueAsString(message);
						tempWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
						tempWriter.write(response);
						tempWriter.flush();
						break;
					case "broadcast":
						log.info("user <{}> broadcast message <{}>", message.getUsername(), message.getContents());
						response = mapper.writeValueAsString(message);
						allSocks = clientMap.values();
						for(Socket i : allSocks) {
							tempWriter = new PrintWriter(new OutputStreamWriter(i.getOutputStream()));
							tempWriter.write(response);
							tempWriter.flush();
						}
						break;
					case "@":
						log.info("user <{}> messaged <{}> to user <{}>", message.getUsername(), message.getContents(), otherUser);
						message.setCommand("@");
						response = mapper.writeValueAsString(message);
						if(clientMap.containsKey(otherUser)) {
							tempWriter = new PrintWriter(new OutputStreamWriter(clientMap.get(otherUser).getOutputStream()));
							tempWriter.write(response);
							tempWriter.flush();
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
						tempWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
						tempWriter.write(response);
						tempWriter.flush();
						break;
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
