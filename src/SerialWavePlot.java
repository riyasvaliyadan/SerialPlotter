import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;

public class SerialWavePlot extends JPanel {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;
    private static final int MAX_POINTS = 500;

    private final LinkedList<Integer> dataPoints = new LinkedList<>();

    public SerialWavePlot() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        Timer timer = new Timer(16, e -> repaint()); // ~60 FPS
        timer.start();
    }

    public void addData(int value) {
        if (dataPoints.size() > MAX_POINTS) {
            dataPoints.removeFirst();
        }
        dataPoints.add(value);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        if (dataPoints.size() < 2) return;

        g.setColor(Color.GREEN);
        int midY = getHeight() / 2;

        int[] xPoints = new int[dataPoints.size()];
        int[] yPoints = new int[dataPoints.size()];

        int i = 0;
        for (Integer val : dataPoints) {
            xPoints[i] = i * getWidth() / MAX_POINTS;
            yPoints[i] = midY - val;
            i++;
        }

        ((Graphics2D) g).setStroke(new BasicStroke(2));
        g.drawPolyline(xPoints, yPoints, dataPoints.size());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Serial Wave Plot");
        SerialWavePlot wavePlot = new SerialWavePlot();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(wavePlot);
        frame.pack();
        frame.setVisible(true);

        // Setup serial port
        SerialPort comPort = SerialPort.getCommPorts()[1]; // Choose the first available port
        comPort.setBaudRate(9600); // Set baud rate (match your device)
        comPort.openPort();

        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            while (comPort.isOpen()) {
                while (comPort.bytesAvailable() > 0) {
                    byte[] buffer = new byte[1];
                    comPort.readBytes(buffer, 1);
                    char c = (char) buffer[0];
                    if (c == '\n') {
                        try {
                            int value = Integer.parseInt(sb.toString().trim());
                            wavePlot.addData(value);
                        } catch (NumberFormatException ignored) {}
                        sb.setLength(0);
                    } else {
                        sb.append(c);
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }
}
