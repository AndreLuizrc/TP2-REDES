package com.mycompany.app;

public class App {
    public static void main(String[] args) {
        System.out.println("=== INICIALIZANDO SISTEMA DE JOGO ===");
        
        // Instancia e inicia o servidor
        Servidor servidor = new Servidor();
        servidor.iniciar();
    }
}   