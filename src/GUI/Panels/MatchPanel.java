package GUI.Panels;

import javax.swing.*;

import Constants.Constants.Dimensions;
import Constants.Constants.Images;
import GUI.DuoRaceTarget;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Console;
import java.util.ArrayList;

import Util.Log;

public class MatchPanel extends JPanel{
	// remember which client we've attached to so we don't register handlers multiple times
	private Network.GameClient attachedClient = null;
	private boolean myTurn;
	private boolean duoRaceStarted = false;
	private Runnable onEndMatch = null;
	// TopCard component to display the current discard top
	private static class TopCard extends GUI.Card {
		private Messages.DuoCard card = null;
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (card != null) {
				// Log.d("TopCard", "painting card: " + card.color + " " + card.value + (card.wildColor!=null?(" (wild as " + card.wildColor + ")"):""));

				java.awt.Image img;
				if (card.wildColor != null && card.color.equals("WILD")) {
					try {
						img = Constants.Constants.Images.getDUOCardImage(card.wildColor.toLowerCase() + "_" + card.value.toLowerCase());
						g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
						return;
					} catch (Exception ex) {
						g.setColor(java.awt.Color.WHITE);
						g.fillRect(0,0,getWidth(),getHeight());
						g.setColor(java.awt.Color.BLACK);
						g.drawString(card.color + " " + card.value, 5, 15);
					}
				}
				else{
					try {
						img = Constants.Constants.Images.getDUOCardImage(card.color.toLowerCase() + "_" + card.value.toLowerCase());
						g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
					} catch (Exception ex) {
						g.setColor(java.awt.Color.WHITE);
						g.fillRect(0,0,getWidth(),getHeight());
						g.setColor(java.awt.Color.BLACK);
						g.drawString(card.color + " " + card.value, 5, 15);
					}
				}
				
			} else {
				g.setColor(java.awt.Color.GRAY);
				g.fillRect(0,0,getWidth(),getHeight());
			}
		}
		public void setCard(Messages.DuoCard c) { this.card = c; repaint();}
	}

	private final JPanel layoutWrapper = new JPanel(new BorderLayout()){
		// Image backgroundGif = Images.MATCH_BG_GIF;
		// Timer timer = new Timer(1000/15, e -> {revalidate(); repaint();});
		// {
		// 	timer.start();
		// }

		// @Override
		// protected void paintComponent(Graphics g) {
		// 	super.paintComponent(g);
		// 	g.drawImage(backgroundGif, 0, 0, getWidth(), getHeight(), this);
		// }
	};

	private final TopCard topCardComponent = new TopCard();
	private final JLabel handsLabel = new JLabel("Hands: -");
	private final JButton drawButton = new JButton("Draw");
	private final JButton playButton = new JButton("Play Card");
	private final JButton skipButton = new JButton("Skip Turn");
	private final JLabel bgLabel;
	private final DuoRaceTarget raceTarget = new DuoRaceTarget();
	private boolean drawRemaining = false;
	//used to detected when my turn starts, in case the server sends multiple state updates in one turn
	private boolean wasMyTurn = false;
	private int myPlayerId = -1;
	private int playerCount = 2; //default to 2, will be updated on match start

	// UI areas
	// custom panel that spreads components evenly across available width
	private class SpreadPanel extends JPanel {
		private final int cardW = Constants.Constants.Dimensions.CARD_WIDTH;
		private final int cardH = Constants.Constants.Dimensions.CARD_HEIGHT;
		private final boolean allowOverlap;
		private final int cwRotation; // degrees clockwise rotation, 0 = none, 90 = vertical
		
		public SpreadPanel(boolean allowOverlap) {
			setOpaque(false);
			this.allowOverlap = allowOverlap;
			this.cwRotation = 0;
		}

		public SpreadPanel(boolean allowOverlap, int ccwRotation) {
			setOpaque(false);
			this.allowOverlap = allowOverlap;
			this.cwRotation = ccwRotation;
		}

		@Override
		public void doLayout() {
			int n = getComponentCount();
			if (n == 0) return;
			int w = getWidth();
			int h = getHeight();
			int cw = cardW; int ch = cardH;
			int gap = (w - n * cw) / (n + 1);
			if (!allowOverlap &&gap < 2) {
				gap = 2;
			}
			int x = gap;
			int y = Math.max(0, (h - ch) / 2);
			for (int i=0;i<n;i++) {
				Component c = getComponent(i);
				c.setBounds(x, y, cw, ch);
				x += cw + gap;
			}
		}

		@Override
		public Dimension getPreferredSize() {
			int n = getComponentCount();
			int w = n * cardW + (n + 1) * 4;

			int rot = Math.abs(cwRotation % 360);
			if (rot == 90 || rot == 270) {
				return new Dimension(cardH + 30, w);
			}
			return new Dimension(w, cardH + 30);
		}

		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		@Override
    	protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g.create();

			// Rotate 90 degrees clockwise around the center
			g2d.rotate(Math.toRadians(cwRotation), getWidth() / 2.0, getHeight() / 2.0);
			//paint(g2d);
			super.paintComponent(g2d);
			g2d.dispose();
		}

		@Override
		protected void paintChildren(Graphics g) {
			Graphics2D g2d = (Graphics2D) g.create();

			// Rotate 90 degrees clockwise around the center
			g2d.rotate(Math.toRadians(cwRotation), getWidth() / 2.0, getHeight() / 2.0);
			//paint(g2d);
			super.paintChildren(g2d);
			g2d.dispose();
		}
	}

	private SpreadPanel[] opponentPanels = null;
	private final SpreadPanel handPanel = new SpreadPanel(false);

	// local hand model
	private final java.util.List<GUI.CardInHand> myHand = new java.util.ArrayList<>() {
		@Override
		public boolean add(GUI.CardInHand c) {
			boolean r = super.add(c);
			// on click, select and deselect all others, also update the play button state
			c.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseClicked(java.awt.event.MouseEvent e) {
					if (e.getSource() instanceof GUI.CardInHand) {
						GUI.CardInHand clicked = (GUI.CardInHand)e.getSource();
						clicked.setSelected(!clicked.isSelected());
						for (GUI.CardInHand o : myHand) {
							if (o != clicked && o.isSelected()) {
								o.setSelected(false);
							}
						}
					}
					playButton.setEnabled(myTurn && hasSelectedLegalCard(lastTopCard));
				}
			});
			return r;
		}
	};

	private Messages.DuoCard lastTopCard = null;

	private final Network.GameClient.DuoStateHandler stateHandler = m -> {
		SwingUtilities.invokeLater(() -> {
			topCardComponent.setPreferredSize(new Dimension(Constants.Constants.Dimensions.CARD_WIDTH*2, Constants.Constants.Dimensions.CARD_HEIGHT*2));
			topCardComponent.setCard(m.topCard);
			Log.d("MatchPanel", "Top card updated: " + m.topCard.toString());
			lastTopCard = m.topCard;

			ArrayList<Integer> handSizes = new ArrayList<>();
			for (int i = 0; i < m.hands.size(); i++) {
				handSizes.add(m.hands.get(i).size());
			}

			handsLabel.setText("Hands: " + handSizes.toString());
			for (int i = 0; i < opponentPanels.length; i++) {
				int opponentId = (myPlayerId + 1 + i) % playerCount;
				opponentPanels[i].removeAll();
				if (opponentId >= 0 && opponentId < handSizes.size()) {
					int count = handSizes.get(opponentId);
					for (int j = 0; j < count; j++) {
						GUI.Card back = GUI.Card.createBack();
						back.setPreferredSize(new Dimension(Constants.Constants.Dimensions.CARD_WIDTH, Constants.Constants.Dimensions.CARD_HEIGHT));
						opponentPanels[i].add(back);
					}
				}
				opponentPanels[i].revalidate(); opponentPanels[i].repaint();
			}


			myHand.clear();
			handPanel.removeAll();
			if (m.hands.get(myPlayerId) != null) {
				for (Messages.DuoCard dc : m.hands.get(myPlayerId)) {
					GUI.CardInHand c = new GUI.CardInHand(dc);
					myHand.add(c);
					handPanel.add(c);
				}
			}
			handPanel.revalidate(); handPanel.repaint();
			// update my hand if changed
			
			

			// enable draw/play only on your turn
			myTurn = (myPlayerId >= 0 && m.currentPlayerId == myPlayerId);
			Log.d("MatchPanel", "myTurn=" + myTurn + ", myPlayerId=" + myPlayerId + ", currentPlayerId=" + m.currentPlayerId);
			//ensures only one draw per turn, button is enabled when turn starts, and is disabled when used
			if(!drawRemaining) drawRemaining = !wasMyTurn && myTurn;
			drawButton.setEnabled(drawRemaining && myTurn);
			
			wasMyTurn = myTurn;

			if (m.duoRace && !duoRaceStarted) {
				Log.d("MatchPanel", "Duo race started");
				duoRaceStarted = true;
				raceTarget.setLocation((int)(Math.random() * (Dimensions.WIDTH - Dimensions.RACE_TARGET_WIDTH)), (int)(Math.random() * (Dimensions.HEIGHT - Dimensions.RACE_TARGET_HEIGHT)));
				raceTarget.setVisible(true);
				add(raceTarget);
				setComponentZOrder(raceTarget, 0); //bring to front
				revalidate(); repaint();
			}

			if (!m.duoRace && duoRaceStarted) {
				Log.d("MatchPanel", "Duo race ended");
				duoRaceStarted = false;
				raceTarget.setVisible(false);
				remove(raceTarget);
				revalidate(); repaint();
			}

			if (m.winnerId != -1) {
				if (m.winnerId == myPlayerId) {
					JOptionPane.showMessageDialog(this, "You have won the game!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(this, "Player " + m.winnerId + " has won the game!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
				}
				if (onEndMatch != null) {
					onEndMatch.run();
				}
			}
		});
	};

	private final Network.GameClient.DuoStartHandler startHandler = m -> {
		SwingUtilities.invokeLater(() -> {
			opponentPanels = new SpreadPanel[m.playerCount - 1];

			if (opponentPanels.length  == 1) {
				opponentPanels[0] = new SpreadPanel(true);
			}
			else {
				for (int i = 0; i < opponentPanels.length; i++) {
					opponentPanels[i] = new SpreadPanel(true, -180 + 90 * (i + 1));
				}
			}
			if (opponentPanels.length == 1) {
				opponentPanels[0].setOpaque(false);
				layoutWrapper.add(opponentPanels[0], BorderLayout.NORTH);
			}
			else{
				String[] layoutPositions = {BorderLayout.WEST, BorderLayout.NORTH, BorderLayout.EAST};
				for (int i = 0; i < opponentPanels.length; i++) {
					SpreadPanel p = opponentPanels[i];
					p.setOpaque(false);
					layoutWrapper.add(p, layoutPositions[i]);
				}
			}
			
			playerCount = m.playerCount;

			myPlayerId = m.assignedPlayerId;
			Log.d("MatchPanel", "Start handler: assignedPlayerId=" + myPlayerId + " startingHandSize=" + (m.startingHand==null?0:m.startingHand.size()));
			myHand.clear();
			handPanel.removeAll();
			if (m.startingHand != null) {
				for (Messages.DuoCard dc : m.startingHand) {
					GUI.CardInHand c = new GUI.CardInHand(dc);
					myHand.add(c);
					handPanel.add(c);
				}
			}
			handPanel.revalidate(); handPanel.repaint();
			// debug: confirm components and sizes
			Log.d("MatchPanel", "postStart: myHand.size=" + myHand.size() + ", handPanel.count=" + handPanel.getComponentCount() + ", handPanel.w=" + handPanel.getWidth() + ", h=" + handPanel.getHeight());
		});
	};

	public void attachClient(Network.GameClient client) {
		if (client == null) return;
		// if already attached to this client, skip
		if (attachedClient == client) {
			Log.d("MatchPanel", "attachClient: already attached to this client (" + client + ")");
			return;
		}
		// detach from previous client if any
		if (attachedClient != null) {
			try {
				attachedClient.removeUnoStartHandler(startHandler);
				attachedClient.removeUnoStateHandler(stateHandler);
				Log.d("MatchPanel", "attachClient: detached from previous client (" + attachedClient + ")");
			} catch (Exception ignored) {}
		}
		attachedClient = client;
		Log.d("MatchPanel", "attaching to client: " + client + " idHash=" + System.identityHashCode(client));
		// defensively remove before adding to avoid duplicates
		client.removeUnoStartHandler(startHandler);
		client.removeUnoStateHandler(stateHandler);
		client.addUnoStartHandler(startHandler);
		client.addUnoStateHandler(stateHandler);
	}

	public MatchPanel() {
		setSize(Dimensions.WIDTH, Dimensions.HEIGHT);

		layoutWrapper.setSize(Dimensions.WIDTH, Dimensions.HEIGHT);
		layoutWrapper.setOpaque(false);
		layoutWrapper.setDoubleBuffered(true);

		setLayout(null);

		JPanel centerContainer = new JPanel(new GridBagLayout());
		centerContainer.setOpaque(false);
		java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0; gbc.gridy = 0;
		gbc.weightx = 1.0; gbc.weighty = 1.0;
		gbc.anchor = java.awt.GridBagConstraints.CENTER;
		topCardComponent.setPreferredSize(new Dimension(Constants.Constants.Dimensions.CARD_WIDTH*2, Constants.Constants.Dimensions.CARD_HEIGHT*2));
		centerContainer.add(topCardComponent, gbc);
		layoutWrapper.add(centerContainer, BorderLayout.CENTER);

		JPanel bottomWrapper = new JPanel(new BorderLayout());
		bottomWrapper.setOpaque(false);
		// bottomWrapper.add(handsLabel, BorderLayout.NORTH);
		handPanel.setOpaque(true);

		JScrollPane handScrollPane = new JScrollPane(handPanel, 
		JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		bottomWrapper.add(handScrollPane, BorderLayout.CENTER);

		JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
		controls.add(drawButton);
		controls.add(playButton);
		controls.add(skipButton);
		bottomWrapper.add(controls, BorderLayout.SOUTH);
		bottomWrapper.setVisible(true);
		layoutWrapper.add(bottomWrapper, BorderLayout.SOUTH);

		drawButton.setEnabled(false);
		playButton.setEnabled(false);
		skipButton.setEnabled(false);

		bgLabel = new JLabel(new ImageIcon(Images.MATCH_BG_GIF));
		bgLabel.setBounds(0,0, Constants.Constants.Dimensions.WIDTH, Constants.Constants.Dimensions.HEIGHT);
		bgLabel.setOpaque(false);
		add(bgLabel);
		setComponentZOrder(bgLabel, getComponentCount()-1); //send to back

		layoutWrapper.setBounds(0,0, Constants.Constants.Dimensions.WIDTH, Constants.Constants.Dimensions.HEIGHT - 35);
		add(layoutWrapper);

		raceTarget.setVisible(false);
		raceTarget.setSize(raceTarget.maxSize);
		raceTarget.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				raceTarget.setVisible(false);
				Messages.WonDuoRaceMessage m = new Messages.WonDuoRaceMessage();
				m.playerId = myPlayerId;
				try {Network.NetworkManager.getInstance().getClient().sendTCP(m); } catch (Exception ex) {System.err.println(ex.getMessage());}
			}
		});

		drawButton.addActionListener(e -> {
			Messages.DrawDuoCardMessage m = new Messages.DrawDuoCardMessage();
			m.playerId = myPlayerId; m.count = 1;
			try { Network.NetworkManager.getInstance().getClient().sendTCP(m); } catch (Exception ex) {System.err.println(ex.getMessage());}
			drawButton.setEnabled(false);
			skipButton.setEnabled(true);
			drawRemaining = false;
		});

		skipButton.addActionListener(e -> {
			Messages.SkipDuoTurnMessage m = new Messages.SkipDuoTurnMessage();
			m.playerId = myPlayerId;
			try { Network.NetworkManager.getInstance().getClient().sendTCP(m); } catch (Exception ex) {System.err.println(ex.getMessage());}
			skipButton.setEnabled(false);
			drawButton.setEnabled(false);
			playButton.setEnabled(false);
			drawRemaining = true;
		});

		playButton.addActionListener(e -> {
			GUI.CardInHand selected = null;
			for (GUI.CardInHand c : myHand) if (c.isSelected()) { selected = c; break; }
			if (selected == null) return;

			Messages.PlayDuoCardMessage p = new Messages.PlayDuoCardMessage();
			p.playerId = -1; p.card = selected.getCardData();
			if (p.card.color.equalsIgnoreCase("WILD")) {
				String[] colors = {"RED", "YELLOW", "GREEN", "BLUE"};
				String chosen = (String)JOptionPane.showInputDialog(this, "Choose a color:", "Wild Card Color",
					JOptionPane.PLAIN_MESSAGE, null, colors, colors[0]);
				System.out.println("chosen color: " + chosen);
				if (chosen == null) chosen = colors[(int)(Math.random() * colors.length)]; // user cancelled, pick randomly
				p.card.wildColor = chosen;
				Log.d("MatchPanel", "p.card.wildcolor = " + p.card.wildColor);
			}
			try { Network.NetworkManager.getInstance().getClient().sendTCP(p); } catch (Exception ex) { }
			// wait for server to broadcast state update before mutating UI
			playButton.setEnabled(false);
			skipButton.setEnabled(false);
			drawButton.setEnabled(false);
			drawRemaining = true;
		});

		// If a client already exists, attach panel handlers (avoid double-registering handlers)
		try {
			Network.GameClient gc = Network.NetworkManager.getInstance().getClient();
			if (gc != null) {
				attachClient(gc);
			}
		} catch (Exception ignored) {}
	}

	@Override
	protected void paintChildren(Graphics g) {
		setComponentZOrder(bgLabel, getComponentCount()-1); 
		super.paintChildren(g);
	}

	private boolean hasSelectedLegalCard(Messages.DuoCard top) {
		if (top == null) {
			Log.d("Match Panel", "Top card is null");
			return false;
		}
		for (GUI.CardInHand c : myHand) {
			if (!c.isSelected()) continue;
			Messages.DuoCard uc = c.getCardData();
			if (uc == null) continue;
			String v = uc.value.toUpperCase();
			String col = uc.color.toUpperCase();
			String topCol = "";
			try{
				topCol = top.color.equals("WILD")? top.wildColor.toUpperCase() : top.color.toUpperCase();
			}
			catch(Exception e){
				e.printStackTrace();
				Log.d("Match Panel", "Invalid top card: " + top.toString());
			}
			if (col.equals("WILD")) return true;
			if (col.equalsIgnoreCase(topCol)) return true;
			if (v.equalsIgnoreCase(top.value)) return true;
		}
		return false;
	}

	public void setStatus(String s) {
		// optional debug helper: set text on top card's tooltip
		topCardComponent.setToolTipText(s);
	}

	public void setOnEndMatch(Runnable r) {onEndMatch = r;}
}
