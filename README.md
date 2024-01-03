# MultiChat Crypted (Local) with Visual Interface in Java

## Overview

This project aims to provide a MultiChat application with a visual interface in Java, incorporating threads and basic security features. The primary goals include enhancing understanding of thread management and security practices in programming.

## Getting Started

To start the MultiChat application, follow these steps:

1. Run the `ChatServer` class first.
2. Then, launch multiple instances of the `ChatGUI` class, where each instance represents a different user.

## Usage

### Commands

- `/nick`: Allows you to change your nickname.
- `/bye`: Makes you quit the chat.
- `/mp name`: Sends a private message to the user with the specified name.

## Server Class (`ChatServer.java`)

- Manages incoming connections from clients.
- Maintains a list of connected clients.
- Broadcasts messages to all clients or sends private messages.

## Client Class (`ChatGUI.java`)

- Provides a visual interface for users.
- Allows users to input commands and messages.
- Displays received messages in the chat window.

## Example Usage

1. Start the server by running the `ChatServer` class.
2. Launch multiple instances of the `ChatGUI` class to simulate different users.
3. Use commands such as `/nick`, `/bye`, and `/mp name` to interact with the chat.

## Note

For simplicity, the security features in this example are minimal. If you intend to use this application in a production environment, consider enhancing the security aspects.
