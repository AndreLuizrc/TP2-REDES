package com.mycompany.app;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Servidor {
    
    public static final int PORTA_TCP = 6789;
    public static final int PORTA_UDP = 9876;
    
    private static final int MAP_SIZE = 20; 
    private char[][] mapa = new char[MAP_SIZE][MAP_SIZE];
    private boolean[][] paredes = new boolean[MAP_SIZE][MAP_SIZE];

    private int[] posP1 = {1, 1};
    private int[] posP2 = {MAP_SIZE-2, MAP_SIZE-2};
    
    private int mortesP1 = 0;
    private int mortesP2 = 0;

    private String dirP1 = "s"; 
    private String dirP2 = "w";

    private long lastShotP1 = 0;
    private long lastShotP2 = 0;
    private final long COOLDOWN_MS = 500; // Cooldown de 2 segundos

    private int jogadoresConectados = 0;
    private Set<SocketAddress> clientesUDP = Collections.synchronizedSet(new HashSet<>());
    private DatagramSocket socketUdp;

    private List<Bala> balas = new CopyOnWriteArrayList<>();

    public void iniciar() {
        criarObstaculos();
        inicializarMapa();
        
        new Thread(() -> rodarUDP()).start();
        new Thread(() -> rodarTCP()).start();
        new Thread(() -> gameLoopBalas()).start();
    }

    private void gameLoopBalas() {
        while (true) {
            try {
                Thread.sleep(100); 
                if (!balas.isEmpty()) {
                    moverBalas();
                    broadcastMapa();
                }
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    // --- AQUI ESTÁ A MÁGICA DA MOVIMENTAÇÃO DIAGONAL ---
    private synchronized void moverBalas() {
        List<Bala> remover = new ArrayList<>();

        for (Bala b : balas) {
            // Verifica a string da direção para saber se move em X, em Y ou nos dois
            
            // Movimento Vertical (W/S)
            if (b.direcao.contains("w")) b.x--;
            if (b.direcao.contains("s")) b.x++;
            
            // Movimento Horizontal (A/D)
            if (b.direcao.contains("a")) b.y--;
            if (b.direcao.contains("d")) b.y++;

            // Verifica limites e paredes
            if (b.x < 0 || b.x >= MAP_SIZE || b.y < 0 || b.y >= MAP_SIZE) {
                remover.add(b); continue;
            }
            if (paredes[b.x][b.y]) {
                remover.add(b); continue;
            }

            // Verifica colisão com Jogadores
            if (b.x == posP1[0] && b.y == posP1[1]) {
                if (b.dono != 1) { 
                    mortesP2++; remover.add(b); respawn(1);
                }
            }
            else if (b.x == posP2[0] && b.y == posP2[1]) {
                if (b.dono != 2) { 
                    mortesP1++; remover.add(b); respawn(2);
                }
            }
        }
        balas.removeAll(remover);
        atualizarPosicoes();
    }

    // --- AQUI ESTÁ A MÁGICA DO SPREAD (TIRO TRIPLO) ---
    private synchronized void processarTiro(int id) {
        long agora = System.currentTimeMillis();
        long ultimaVez = (id == 1) ? lastShotP1 : lastShotP2;

        if (agora - ultimaVez < COOLDOWN_MS) return;

        if (id == 1) lastShotP1 = agora; else lastShotP2 = agora;

        int[] pos = (id == 1) ? posP1 : posP2;
        String dir = (id == 1) ? dirP1 : dirP2;
        
        // Adiciona 3 balas dependendo da direção que o jogador olha
        // Bala Central (Vai reto)
        balas.add(new Bala(pos[0], pos[1], dir, id));

        // Balas Diagonais
        switch (dir) {
            case "w": // Olhando pra cima
                balas.add(new Bala(pos[0], pos[1], "wa", id)); // Noroeste
                balas.add(new Bala(pos[0], pos[1], "wd", id)); // Nordeste
                break;
            case "s": // Olhando pra baixo
                balas.add(new Bala(pos[0], pos[1], "sa", id)); // Sudoeste
                balas.add(new Bala(pos[0], pos[1], "sd", id)); // Sudeste
                break;
            case "a": // Olhando pra esquerda
                balas.add(new Bala(pos[0], pos[1], "aw", id)); // Noroeste
                balas.add(new Bala(pos[0], pos[1], "as", id)); // Sudoeste
                break;
            case "d": // Olhando pra direita
                balas.add(new Bala(pos[0], pos[1], "dw", id)); // Nordeste
                balas.add(new Bala(pos[0], pos[1], "ds", id)); // Sudeste
                break;
        }
    }

    private void respawn(int id) {
        if (id == 1) {
            posP1[0] = 1; posP1[1] = 1; dirP1 = "s";
        } else {
            posP2[0] = MAP_SIZE-2; posP2[1] = MAP_SIZE-2; dirP2 = "w";
        }
    }

    private void criarObstaculos() {
        for(int i=0; i<MAP_SIZE; i++) Arrays.fill(paredes[i], false);
        for(int i=5; i<15; i++) { paredes[i][5] = true; paredes[i][14] = true; }
        paredes[9][9] = true; paredes[9][10] = true; paredes[10][9] = true; paredes[10][10] = true;
    }

    private synchronized void moverJogador(int id, String direcao) {
        int[] pos = (id == 1) ? posP1 : posP2;
        int novoX = pos[0];
        int novoY = pos[1];

        if (id == 1) dirP1 = direcao; else dirP2 = direcao;

        switch (direcao.toLowerCase()) {
            case "w": novoX--; break;
            case "s": novoX++; break;
            case "a": novoY--; break;
            case "d": novoY++; break;
        }

        if (novoX >= 0 && novoX < MAP_SIZE && novoY >= 0 && novoY < MAP_SIZE) {
            if (!paredes[novoX][novoY]) {
                pos[0] = novoX; pos[1] = novoY;
                atualizarPosicoes(); broadcastMapa();
            }
        }
    }

    private void rodarTCP() {
        try (ServerSocket server = new ServerSocket(PORTA_TCP)) {
            while (true) {
                Socket socket = server.accept();
                jogadoresConectados++;
                DataOutputStream saida = new DataOutputStream(socket.getOutputStream());
                int idParaDar = (jogadoresConectados <= 1) ? 1 : 2;
                saida.writeBytes(idParaDar + "\n");
                socket.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void rodarUDP() {
        try {
            socketUdp = new DatagramSocket(PORTA_UDP);
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                    socketUdp.receive(pacote);
                    clientesUDP.add(pacote.getSocketAddress());
                    String msg = new String(pacote.getData(), 0, pacote.getLength());
                    if (msg.trim().isEmpty()) continue;
                    String[] partes = msg.split(";");
                    if (partes.length == 2) {
                        if(partes[1].equals("PING")) broadcastMapa();
                        else {
                            int id = Integer.parseInt(partes[0]);
                            if (partes[1].equals("SHOOT")) processarTiro(id);
                            else moverJogador(id, partes[1]);
                        }
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void broadcastMapa() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=== SHOTGUN ARENA ===\n");
            sb.append(" P1: ").append(mortesP1).append(" | P2: ").append(mortesP2).append("\n\n");
            
            for (int i = 0; i < MAP_SIZE; i++) {
                for (int j = 0; j < MAP_SIZE; j++) {
                    sb.append(" ").append(mapa[i][j]).append(" ");
                }
                sb.append("\n");
            }
            byte[] dados = sb.toString().getBytes();
            synchronized (clientesUDP) {
                for (SocketAddress cliente : clientesUDP) {
                    DatagramPacket pacote = new DatagramPacket(dados, dados.length, cliente);
                    socketUdp.send(pacote);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void inicializarMapa() { atualizarPosicoes(); }

    private synchronized void atualizarPosicoes() {
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                mapa[i][j] = paredes[i][j] ? '#' : '.';
            }
        }
        for (Bala b : balas) {
             if (b.x >= 0 && b.x < MAP_SIZE && b.y >= 0 && b.y < MAP_SIZE) {
                 if(!paredes[b.x][b.y]) mapa[b.x][b.y] = '*';
             }
        }
        mapa[posP1[0]][posP1[1]] = '1';
        mapa[posP2[0]][posP2[1]] = '2';
    }

    class Bala {
        int x, y;
        String direcao;
        int dono;
        public Bala(int x, int y, String direcao, int dono) {
            this.x = x; this.y = y; this.direcao = direcao; this.dono = dono;
        }
    }
}