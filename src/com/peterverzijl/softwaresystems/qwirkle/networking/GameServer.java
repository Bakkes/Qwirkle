package com.peterverzijl.softwaresystems.qwirkle.networking;

import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.peterverzijl.softwaresystems.qwirkle.Block;
import com.peterverzijl.softwaresystems.qwirkle.Game;
import com.peterverzijl.softwaresystems.qwirkle.Player;
import com.peterverzijl.softwaresystems.qwirkle.networking.exceptions.AddPlayerToGameException;
import com.peterverzijl.softwaresystems.qwirkle.networking.exceptions.GameFullException;
import com.peterverzijl.softwaresystems.qwirkle.exceptions.NotYourBlockException;
import com.peterverzijl.softwaresystems.qwirkle.exceptions.NotYourTurnException;

/**
 * Handles a single game on the server, with a certain amount of players.
 * 
 * @author Peter Verzijl
 * @version 1.0a
 */
public class GameServer implements Server {

	private boolean hasGameStarted = false; // If the game has started

	private int mTargetPlayerCount; // The amount of players wanted in this
									// game.
	private Game mGame; // The game played
	private List<ClientHandler> mClients; // The client handlers

	private Map<ClientHandler, Player> playerClientMap;

	/**
	 * Constructor
	 * 
	 * @param targetPlayerCount
	 *            The amount of wanted players on this server.
	 */
	public GameServer(int targetPlayerCount) {
		mTargetPlayerCount = targetPlayerCount;

		mClients = new ArrayList<ClientHandler>();
		playerClientMap = new HashMap<ClientHandler, Player>();
	}

	/**
	 * Adds a client to the client list of the GameServer
	 * 
	 * @param client
	 *            The client to add.
	 */
	public void addClient(ClientHandler client) {
		if (!mClients.contains(client)) {
			mClients.add(client);
		}
	}

	/**
	 * Returns the amount of players in this server game.
	 * 
	 * @return The amount of players in this server game.
	 */
	public int getNumPlayers() {
		return mClients.size();
	}

	/**
	 * Processes all incoming messages for the game state of the server.
	 * 
	 * @param message
	 *            The message to process.
	 * @param client
	 *            The client handler that send the message.
	 */
	@Override
	public void sendMessage(String message, ClientHandler client) {
		if (message.length() < 1) {
			return;
		}

		String[] parameters = message.split("" + Protocol.Server.Settings.DELIMITER);
		String command = parameters[0];
		// Remove command from parameters
		parameters = Arrays.copyOfRange(parameters, 1, parameters.length);

		switch (command) {
		case Protocol.Client.CHANGESTONE:
			// Get stones from command
			try {
				List<Block> tradedBlocks = new ArrayList<Block>();
				for (String stone : parameters) {
					// TODO (Peter) : Get the stone from his hand instead of a
					// copy.
					Block b = Block.getBlockFromCharPair(stone);
					if (b != null) {
						// Check if the current player has this block.
						Block newBlock = mGame.tradeBlock(playerClientMap.get(client), b);
						tradedBlocks.add(newBlock);
					} else {
						client.sendMessage(Protocol.Server.ERROR + 
											Protocol.Server.Settings.DELIMITER + 2);
					}
					sendBlocksClient(tradedBlocks, client);
				}
			} catch (NotYourTurnException e) {
				client.sendMessage(Protocol.Server.ERROR + 
									Protocol.Server.Settings.DELIMITER + 1);
			} catch (NotYourBlockException e) {
				client.sendMessage(Protocol.Server.ERROR + 
									Protocol.Server.Settings.DELIMITER + 2);
			}
			break;
		case Protocol.Client.CHAT:
			broadcast(Protocol.Server.CHAT + 
						Protocol.Server.Settings.DELIMITER + 
						client.getName() + ": "
						+ parameters[0]); // The first parameter is the text.
			break;
		case Protocol.Client.GETSTONESINBAG:
			if (hasGameStarted) {
				client.sendMessage(Protocol.Server.CHAT + 
									Protocol.Server.Settings.DELIMITER + 
									mGame.getNumStonesInBag());
			} else {
				client.sendMessage(Protocol.Server.ERROR + 
									Protocol.Server.Settings.DELIMITER + 8);
			}
			break;
		case Protocol.Client.MAKEMOVE:
			
			// MAKEMOVE_<CharChar*int*int>_<CharChar*int*int>
			break;
		case Protocol.Client.QUIT:
			// Ok doei lol
			mClients.remove(client);
			mGame.removePlayer(playerClientMap.get(client));
			client.shutdown();
			break;
		default:
			client.sendMessage(Protocol.Server.ERROR + 
								Protocol.Server.Settings.DELIMITER + 8);
			break;
		}
	}
	
	/**
	 * Sends an ADDTOHAND message to the client.
	 * @param blocks The blocks to send to the client.
	 * @param client The receiver of the blocks.
	 */
	private void sendBlocksClient(List<Block> blocks, ClientHandler client) {
		String message = "";
		message += Protocol.Server.ADDTOHAND;
		for (Block b : blocks) {
			message += Protocol.Server.Settings.DELIMITER;
			message += Block.toChars(b);
		}
		client.sendMessage(message);
	}

	/**
	 * Broadcast a message to all the clients.
	 * 
	 * @param message
	 *            The message to broadcast.
	 */
	@Override
	public void broadcast(String message) {
		if (message != null) {
			for (ClientHandler client : mClients) {
				client.sendMessage(message);
			}
		}
	}

	/**
	 * @return Returns the target amount of players in this game.
	 */
	public int getTargetPlayerCount() {
		return mTargetPlayerCount;
	}

	/**
	 * Adds a client to the game.
	 * 
	 * @param client
	 *            The client to add to the game.
	 * @throws AddPlayerToGameException
	 */
	public void addPlayer(ClientHandler client) throws GameFullException {
		if (!hasGameStarted && mClients.size() <= mTargetPlayerCount) {
			addClient(client);
			client.setServer(this);
			// Check if we can now start the game
			if (mClients.size() == mTargetPlayerCount) {
				startGame();
			} else {
				broadcast(Protocol.Server.OKWAITFOR + Protocol.Server.Settings.DELIMITER
						+ (mTargetPlayerCount - mClients.size()));
			}
		} else {
			throw new GameFullException();
		}
	}

	/**
	 * Start the game.
	 */
	private void startGame() {
		// Send start game message
		String message = Protocol.Server.STARTGAME;
		for (ClientHandler client : mClients) {
			message += Protocol.Server.Settings.DELIMITER;
			message += client.getName();
		}
		broadcast(message);

		hasGameStarted = true;

		// TODO (peter) : make new game
		// Create a new player for every client.
		List<Player> players = new ArrayList<Player>();
		for (ClientHandler client : mClients) {
			Player p = new Player();
			players.add(p);
			playerClientMap.put(client, p);
		}
		mGame = new Game(players);
		
		// Send blocks to the clients
		for (ClientHandler client : mClients) {
			sendBlocksClient(playerClientMap.get(client).getHand(), client);
			
			// Ask all players to send their initial move.
			String initMoveMessage = "";
			initMoveMessage += Protocol.Server.MOVE;
			initMoveMessage += Protocol.Server.Settings.DELIMITER;
			initMoveMessage += client.getName();
			initMoveMessage += Protocol.Server.Settings.DELIMITER;
			initMoveMessage += client.getName();
			client.sendMessage(initMoveMessage);
		}
		
		// Init the first move...
	}

	/**
	 * Removes a client from our list of clients.
	 * 
	 * @param client
	 *            The client that wants to disconnect.
	 */
	@Override
	public void removeHanlder(ClientHandler client) {
		mClients.remove(client);
	}
}