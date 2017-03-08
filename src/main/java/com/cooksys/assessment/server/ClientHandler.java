package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;

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
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			PrintWriter tempWriter;
			String response;
			String command;
			String otherUser = "";

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				
				command = message.getCommand();
				if(command.startsWith("@")) {
					otherUser = command.substring(1, command.length());
					command = "@";
				}
				
				switch (command) {
					case "connect":
						clientMap.put(message.getUsername(), socket);
						log.info("user <{}> connected", message.getUsername());
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
					case "broadcast":
						log.info("user <{}> broadcast message <{}>", message.getUsername(), message.getContents());
						response = mapper.writeValueAsString(message);
						Collection<Socket> allSocks = clientMap.values();
						for(Socket i : allSocks) {
							tempWriter = new PrintWriter(new OutputStreamWriter(i.getOutputStream()));
							tempWriter.write(response);
							tempWriter.flush();
						}
						break;
					case "@":
						response = mapper.writeValueAsString(message);
						if(clientMap.containsKey(otherUser)) {
							tempWriter = new PrintWriter(new OutputStreamWriter(clientMap.get(otherUser).getOutputStream()));
							tempWriter.write(response);
							tempWriter.flush();
						}
						break;
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
