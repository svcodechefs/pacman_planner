package Game;

import Main.Config;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class Board extends JPanel implements IBoardStateObserver {

    private Config config;

    /*Variables about the screenSize (width = colAmount * blockSize and height = rowAmount * blockSize)*/
    private int screenWidth;
    private int screenHeight;

    /*Images*/
    private Image ghostImage;
    private Image pacmanImage;

    /*Necessary objects for turn-based game*/
    private final Object lock = new Object();

    private Action selectedAction = null;
    private static Board instance = null;

    // Those will be updated by stateUpdate function
    private Position pacmanPosition;
    private List<Position> monsterPositions;
    private short[] boardData;

    public static Board getInstance() {
        if (instance == null)
            instance = new Board();
        return instance;
    }

    @Override
    public void initialize(int rowAmount, int colAmount) {
        int blockSize = config.getInt("block_size");
        this.screenWidth = colAmount * blockSize;
        this.screenHeight = rowAmount * blockSize;
    }

    @Override
    public void finishSignal() {

    }

    private Board() {

        config = Config.getInstance();

        //Load Images
        try {
            ghostImage = new ImageIcon(getClass().getClassLoader().getResource("ghost.png")).getImage();
            pacmanImage = new ImageIcon(getClass().getClassLoader().getResource("pacman.png")).getImage();
        }
        catch(Exception e) {
            throw new RuntimeException("Could not load the images");
        }

        addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                /*If Pacman is not controlled by keyboard, we should not listen for keys*/
                if (config.getBoolean("ai_enabled"))
                    return;

                int key = e.getKeyCode();

                if (key == KeyEvent.VK_LEFT)
                    selectedAction = Action.LEFT;
                else if (key == KeyEvent.VK_RIGHT)
                    selectedAction = Action.RIGHT;
                else if (key == KeyEvent.VK_UP)
                    selectedAction = Action.UP;
                else if (key == KeyEvent.VK_DOWN)
                    selectedAction = Action.DOWN;
                else if (key == KeyEvent.VK_N)
                    selectedAction = Action.STOP;

                synchronized (lock) {
                    if (selectedAction != null)
                        lock.notify();
                }
            }
        });

        setFocusable(true);
        setBackground(config.getColor("cell_color"));
        setDoubleBuffered(true);
    }

    private void drawMaze(Graphics2D g2d) {

        short i = 0;
        int x, y;
        int blockSize = config.getInt("block_size");

        for (y = 0; y < screenHeight; y += blockSize) {
            for (x = 0; x < screenWidth; x += blockSize) {

                g2d.setColor(config.getColor("maze_color"));
                g2d.setStroke(new BasicStroke(2));

                if ((boardData[i] & 1) != 0) {
                    g2d.drawLine(x, y, x, y + blockSize - 1);
                }

                if ((boardData[i] & 2) != 0) {
                    g2d.drawLine(x, y, x + blockSize - 1, y);
                }

                if ((boardData[i] & 4) != 0) {
                    g2d.drawLine(x + blockSize - 1, y, x + blockSize - 1,
                            y + blockSize - 1);
                }

                if ((boardData[i] & 8) != 0) {
                    g2d.drawLine(x, y + blockSize - 1, x + blockSize - 1,
                            y + blockSize - 1);
                }

                if ((boardData[i] & 16) != 0) {
                    g2d.setColor(config.getColor("dot_color"));
                    g2d.fillRect(x + 11, y + 11, 2, 2);
                }

                i++;
            }
        }
    }

    @Override
    public void stateUpdateSignal(Position pacmanPosition, List<Position> monsterPositions, short[] boardData) {
        this.pacmanPosition = pacmanPosition;
        this.monsterPositions = monsterPositions;
        this.boardData = boardData;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;
        int blockSize = config.getInt("block_size");

        g2d.setColor(config.getColor("cell_color"));
        g2d.fillRect(0, 0, this.screenWidth, this.screenHeight);

        drawMaze(g2d);
        g2d.drawImage(pacmanImage, pacmanPosition.x * blockSize + 1, pacmanPosition.y * blockSize + 1, this);

        for (Position monsterPosition : monsterPositions)
            g2d.drawImage(ghostImage, monsterPosition.x * blockSize + 1, monsterPosition.y * blockSize + 1, this);

        g2d.drawImage(null, 5, 5, this);
        Toolkit.getDefaultToolkit().sync();
        g2d.dispose();
    }

    //This function waits until a keyboard button has been clicked
    //When a button is clicked, it wakes up and gets the clicked button
    public Action moveAgentByKeyboard(BoardState state) {
        try {
            synchronized (lock) {
                while (selectedAction == null)
                    lock.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Action decidedAction = selectedAction;

        synchronized (lock) {
            selectedAction = null;
        }

        if (state.checkActionValidity(state.pacman.getCurrentPosition(), decidedAction))
            return decidedAction;

        return Action.STOP;
    }
}