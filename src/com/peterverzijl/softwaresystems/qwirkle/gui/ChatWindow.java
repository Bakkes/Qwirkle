package com.peterverzijl.softwaresystems.qwirkle.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.net.InetAddress;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.peterverzijl.softwaresystems.qwirkle.networking.Client;
import com.peterverzijl.softwaresystems.qwirkle.networking.Server;
import com.peterverzijl.softwaresystems.qwirkle.ui.ChatView;

/**
 * A chat window GUI class that extends the JFrame.
 * @author Peter Verzijl
 * @version 1.0a
 */
@SuppressWarnings("serial")
public class ChatWindow extends JFrame implements ChatView {
	
	public String username = "John Doe";
	
	private Thread		mClientThread;
	private JButton 	mSendMessageButton;	// Sends the typed message
	private JTextField 	mMessageField;		// Input field for message
	private JTextArea	mChatBox;			// Shows the all recieved and send messages
	
	private Client mClient;
	
	public ChatWindow(String windowName) {
		super(windowName);
		
		// Add a client
		try {
			mClient = new Client(InetAddress.getByName("localhost"), Server.PORT, this);
			mClientThread = new Thread(mClient);
			mClientThread.start();
		} catch (IOException e) {
			// Return and don't create anything
			System.out.println("Error: could not create client object. Exiting chat window. Due to: " + e.getMessage());
			return;
		}
		
		// Set border layout
		setLayout(new BorderLayout());
			
		// Create a layout for the button and the text area
		JPanel bottomPanel = new JPanel();
		// Set height of chat panel and button
		{
			Dimension d = bottomPanel.getPreferredSize();
			d.height = 40;
			bottomPanel.setPreferredSize(d);
		}
		bottomPanel.setLayout(new GridBagLayout());
		
		// Create message text field
		mMessageField = new JTextField(40);
		mMessageField.requestFocusInWindow();
		
		// Make and add the button
		mSendMessageButton = new JButton("Send");
		mSendMessageButton.addActionListener(
				new SendButtonAction());
		
		// Create Chat box
		mChatBox = new JTextArea();
		mChatBox.setEditable(false);
		mChatBox.setFont(new Font("Serif", Font.PLAIN, 15));
		mChatBox.setLineWrap(true);
		
		this.add(new JScrollPane(mChatBox), BorderLayout.CENTER);
		
		// Create dynamic text area and fixed size button
		// Flexible layout
		GridBagConstraints left = new GridBagConstraints();
		left.anchor = GridBagConstraints.LINE_START;
		left.fill = GridBagConstraints.BOTH;
		left.weightx = 512.0D;
		left.weighty = 1.0D;
		// Fixed layout
		GridBagConstraints right = new GridBagConstraints();
		right.insets = new Insets(0, 1, 0, 0);	// Inside margins
		right.anchor = GridBagConstraints.LINE_END;
		right.fill = GridBagConstraints.BOTH;
		right.weightx = 50.0D;
		right.weighty = 1.0D;
		
		bottomPanel.add(mMessageField, left);
		bottomPanel.add(mSendMessageButton, right);
		
		// Add south component
		this.add(BorderLayout.SOUTH, bottomPanel);
		
		// Finally set visible
		this.setSize(470, 300);
		this.setVisible(true);
		
		// Add window close listener
		this.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		        if (JOptionPane.showConfirmDialog(new JFrame(), 
		            "Are you sure to close this window?", "Really Closing?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
		            MainWindow.chatWindow = null;
		            try {
						mClient.shutdown();
						mClientThread.join();
					} catch (InterruptedException e) {
						System.out.println("Error, could not close chat. Due to: " + e.getMessage());
					}
		        }
		    }
		});
	}
	
	/**
	 * Adds a message to the chat view.
	 */
	public void addMessage(String message) {
		mChatBox.append(message + "\n");
	}
		
	class SendButtonAction extends AbstractAction {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (mMessageField != null) {
				String message = mMessageField.getText();
				if (message.length() < 1) {
					// TODO (peter) : Show error box.
					System.out.println("Message is too short!");
				} else if (message.equals("/clear")) {
					mChatBox.setText("Cleared all messages\n");
					mMessageField.setText("");
				} else {
					// Add message to client
					mClient.sendChatMessage(message);
					mMessageField.setText("");
				}
				mMessageField.requestFocusInWindow();
			}
		}
	}
}
