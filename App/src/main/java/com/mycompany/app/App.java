package com.mycompany.app;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Inicializando Servidores...");

        // Inicia o servidor UDP em uma nova Thread
        Thread udpThread = new Thread(new UdpServerRunnable());
        udpThread.start();

        // Inicia o servidor TCP em uma nova Thread
        Thread tcpThread = new Thread(new TcpServerRunnable());
        tcpThread.start();
    }

    static class UdpServerRunnable implements Runnable {
        @Override
        public void run() {
            try {
                System.out.println("Servidor UDP iniciado na porta " + Servidor.portaServidorUdp);
                Servidor.udp(null); // O método udp não usa os argumentos, pode ser null
            } catch (Exception e) {
                System.err.println("Erro ao iniciar servidor UDP: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    static class TcpServerRunnable implements Runnable {
        @Override
        public void run() {
            try {
                System.out.println("Servidor TCP iniciado na porta " + Servidor.portaServidorTCP);
                Servidor.tcp(null); // O método tcp não usa os argumentos, pode ser null
            } catch (Exception e) {
                System.err.println("Erro ao iniciar servidor TCP: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
