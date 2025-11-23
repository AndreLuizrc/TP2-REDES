package com.mycompany.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Cliente extends JFrame {
    
    private static final int PORTA_TCP_DESTINO = 6789;
    private static final int PORTA_UDP_DESTINO = 9876;
    private static final String HOST = "localhost";
    
    private JTextArea areaDoJogo;
    private int meuId;
    private DatagramSocket socketUdp;
    private InetAddress ipServidor;

    public static void main(String[] args) {
        new Cliente();
    }

    public Cliente() {
        try {
            configurarRede();
            configurarJanela();
            iniciarEscutaUDP(); // Thread para receber o mapa
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao conectar: " + e.getMessage());
            System.exit(1);
        }
    }

    private void configurarRede() throws Exception {
        // 1. TCP: Pegar ID
        Socket socketTcp = new Socket(HOST, PORTA_TCP_DESTINO);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(socketTcp.getInputStream()));
        String idStr = entrada.readLine();
        meuId = Integer.parseInt(idStr);
        socketTcp.close();

        // 2. Preparar UDP
        socketUdp = new DatagramSocket();
        ipServidor = InetAddress.getByName(HOST);

        // Envia um pacote "PING" vazio só para o servidor salvar nosso IP/Porta
        enviarPacote("PING");
    }

    private void configurarJanela() {
        setTitle("Jogador " + meuId + " (Use W, A, S, D)");
        setSize(650, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Área de texto onde o mapa será desenhado
        areaDoJogo = new JTextArea();
        areaDoJogo.setFont(new Font("Monospaced", Font.BOLD, 20)); // Fonte monoespaçada para alinhar o mapa
        areaDoJogo.setEditable(false);
        areaDoJogo.setBackground(Color.BLACK);
        areaDoJogo.setForeground(Color.GREEN);
        add(new JScrollPane(areaDoJogo));

        // Captura de Teclas (Sem Enter!)
// ... (imports iguais) ...

// Procure a parte do KeyAdapter dentro de configurarJanela() e adicione o CASE do ESPAÇO:

        areaDoJogo.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                String comando = "";
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W: comando = "w"; break;
                    case KeyEvent.VK_S: comando = "s"; break;
                    case KeyEvent.VK_A: comando = "a"; break;
                    case KeyEvent.VK_D: comando = "d"; break;
                    case KeyEvent.VK_SPACE: comando = "SHOOT"; break; // NOVO COMANDO
                }
                
                if (!comando.isEmpty()) {
                    try {
                        enviarPacote(comando);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
// ... (resto do código igual) ...

        setVisible(true);
        areaDoJogo.requestFocus(); // Foca na janela para capturar teclas
    }

    private void enviarPacote(String acao) throws IOException {
        String msg = meuId + ";" + acao;
        byte[] dados = msg.getBytes();
        DatagramPacket pacote = new DatagramPacket(dados, dados.length, ipServidor, PORTA_UDP_DESTINO);
        socketUdp.send(pacote);
    }

    private void iniciarEscutaUDP() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[2048];
                while (true) {
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                    socketUdp.receive(pacote);
                    String mapaRecebido = new String(pacote.getData(), 0, pacote.getLength());
                    
                    // Atualiza a interface gráfica
                    SwingUtilities.invokeLater(() -> {
                        areaDoJogo.setText(mapaRecebido);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}