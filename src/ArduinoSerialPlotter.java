import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public class ArduinoSerialPlotter extends JPanel {
    private int latestValue = 0;
    private final int width = 1000;
    private final int height = 600;

    public ArduinoSerialPlotter() {
        setPreferredSize(new Dimension(width, height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int centerX = (getWidth()/2) - latestValue/2;
        int centerY = (getHeight()/2) - latestValue/2;

        g.fillOval(centerX, centerY, latestValue, latestValue); // Draw circle based on value
        g.setColor(Color.WHITE);

        g.drawString(String.valueOf(latestValue), getWidth()/2, getHeight()/2);
    }

    public void updateValue(int newValue) {
        this.latestValue = newValue;
        repaint(); // Repaint the canvas
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Arduino Serial Visualizer");
        ArduinoSerialPlotter canvas = new ArduinoSerialPlotter();
        frame.add(canvas);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // print comm ports
        for (SerialPort serialPort: SerialPort.getCommPorts()) {
            System.out.println("serialPort = " + serialPort.getVendorID());
        }

        // Serial Port Setup
        SerialPort comPort = SerialPort.getCommPorts()[1]; // Pick first available port
        comPort.setBaudRate(9600);
        comPort.openPort();

        // Thread to read serial data
        new Thread(() -> {
            InputStream in = comPort.getInputStream();
            StringBuilder sb = new StringBuilder();
            try {
                while (true) {
                    while (in.available() > 0) {
                        char c = (char) in.read();
                        if (c == '\n') {
                            try {
                                int value = Integer.parseInt(sb.toString().trim());
                                SwingUtilities.invokeLater(() -> canvas.updateValue(value));
                            } catch (NumberFormatException e) {
                                // skip malformed data
                            }
                            sb.setLength(0); // reset
                        } else {
                            sb.append(c);
                        }
                    }
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}