# TCP Group Chat Application

A real-time group chat application built with **Java Sockets (TCP)** and **JavaFX**. Multiple clients connect to a central server and exchange messages in a shared chat environment.

---

## Requirements

- **JDK 21** or later
- **Maven** 3.6+

---

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/smoothSpikes/messagingApp.git
cd messagingApp
```

### 2. Start the server

**Windows** (double-click or run in terminal):

```
run-server.bat
```

**macOS / Linux**:

```bash
chmod +x run-server.sh
./run-server.sh
```

In the server window, click **Start** to begin accepting connections.

### 3. Start the client(s)

**Windows**:

```
run-client.bat
```

**macOS / Linux**:

```bash
chmod +x run-client.sh
./run-client.sh
```

Open multiple client windows to simulate a group chat.

---

## Using the Application

### Client

1. Enter a **username** (or leave empty for read-only mode).
2. Click **Connect**.
3. Type messages and press **Enter** or click **Send**.
4. Use the **All Users** button or type `allUsers` to see active users.
5. Type `bye` or `end` to disconnect.

### Server

- Click **Start** to begin listening for clients.
- The log area shows activity (connections, messages, etc.).
- The user list shows connected usernames with distinct colors.
- Click **Stop** to shut down the server.

---

## Configuration

Server and client settings are loaded from configuration files:

| File | Location | Keys |
|------|----------|------|
| Server | `tcpServer/src/main/resources/server.properties` | `server.port` (default: 6666) |
| Client | `tcpClient/src/main/resources/client.properties` | `server.host`, `server.port` |

Edit these files to change the port or server address without recompiling.

---

## Project Structure

```
messagingApp-main/
├── tcpServer/           # Server application
│   └── src/main/java/assign/project/
│       ├── ServerApp.java        # JavaFX server UI
│       ├── ServerModel.java      # Server logic (model)
│       ├── ClientHandlerThread.java
│       └── tcpServer.java        # Console entry point
├── tcpClient/           # Client application
│   └── src/main/java/assign/project/
│       ├── ChatClientApp.java    # JavaFX client UI
│       ├── ChatClient.java       # Client logic (model)
│       └── tcpClient.java        # Console entry point
├── run-server.bat       # Run server (Windows)
├── run-client.bat       # Run client (Windows)
├── run-server.sh        # Run server (macOS/Linux)
├── run-client.sh        # Run client (macOS/Linux)
└── README.md
```

---

## Running with Maven (alternative)

From the project root:

```bash
# Server
cd tcpServer && mvn javafx:run

# Client (in another terminal)
cd tcpClient && mvn javafx:run
```

---

## Features

- **Authentication**: Username required; empty username = read-only mode
- **Real-time messaging**: Send and receive messages instantly
- **Active user list**: Command `allUsers` or button to list connected users
- **Visual status**: Online/Offline indicator on client; colored user list on server
- **Model-View separation**: Business logic independent of JavaFX UI

---
