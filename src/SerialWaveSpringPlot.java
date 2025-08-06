import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;

public class SerialWaveSpringPlot extends JPanel {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;
    private static final int MAX_POINTS = 500;

    private final LinkedList<SpringPoint> dataPoints = new LinkedList<>();

    public SerialWaveSpringPlot() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        Timer timer = new Timer(16, e -> repaint()); // ~60 FPS
        timer.start();
    }

    // Spring-based animated point
    private static class SpringPoint {
        double current;
        double target;
        double velocity;

        void update() {
            double stiffness = 0.2;
            double damping = 0.75;
            double force = (target - current) * stiffness;
            velocity += force;
            velocity *= damping;
            current += velocity;
        }
    }

    public void addData(int value) {
        if (dataPoints.size() >= MAX_POINTS) {
            dataPoints.removeFirst();
        }

        SpringPoint point = new SpringPoint();
        point.target = value;

        if (!dataPoints.isEmpty()) {
            point.current = dataPoints.getLast().current; // continue from last value
        } else {
            point.current = value;
        }

        dataPoints.add(point);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        if (dataPoints.size() < 2) return;

        int midY = getHeight() / 2;
        int[] xPoints = new int[dataPoints.size()];
        int[] yPoints = new int[dataPoints.size()];

        int i = 0;
        for (SpringPoint pt : dataPoints) {
            pt.update(); // animate
            xPoints[i] = i * getWidth() / MAX_POINTS;
            yPoints[i] = midY - (int) pt.current;
            i++;
        }

        g.setColor(Color.GREEN);
        ((Graphics2D) g).setStroke(new BasicStroke(2));
        g.drawPolyline(xPoints, yPoints, dataPoints.size());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Serial Wave with Spring Animation");
        SerialWaveSpringPlot wavePlot = new SerialWaveSpringPlot();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(wavePlot);
        frame.pack();
        frame.setVisible(true);

        // Auto-select first available serial port
        SerialPort comPort = SerialPort.getCommPorts()[1];
        comPort.setBaudRate(9600);
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
                        } catch (NumberFormatException ignored) {
                        }
                        sb.setLength(0);
                    } else {
                        sb.append(c);
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }
}
