package com.mycompany.app;

import java.io.*;
import java.net.*;

class Servidor
{
   private static int portaServidor = 9871;
   private static byte[] receiveData = new byte[1024];
   private static byte[] sendData = new byte[1024];

   public static void udp(String args[]) throws Exception
   {
      DatagramSocket serverSocket = new DatagramSocket(portaServidor);

      while(true) 
      {
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

         System.out.println("Aguardando datagrama do cliente....");
         serverSocket.receive(receivePacket);

         System.out.println("RECEIVED: " + new String(receivePacket.getData()));
         InetAddress ipCliente = receivePacket.getAddress();
         int portaCliente = receivePacket.getPort();
         sendData = (new String(receivePacket.getData())).toUpperCase().getBytes();

         DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipCliente, portaCliente);
         serverSocket.send(sendPacket);
         System.out.println("Enviado...");
      }
      
   }

   public static void tcp(String argv[]) throws Exception
   {
      //Efetua as primitivas socket e bind, respectivamente
      ServerSocket socket = new ServerSocket(portaServidor);

      while(true)
      {
         //Efetua as primitivas de listen e accept, respectivamente
         Socket conexao = socket.accept();

         //Efetua a primitiva receive
         System.out.println("Aguardando datagrama do cliente....");
         BufferedReader entrada =  new BufferedReader(new InputStreamReader(conexao.getInputStream()));

         //Operacao com os dados recebidos e preparacao dos a serem enviados
         String str = entrada.readLine();
         System.out.println("Received: " + str);

         str = str.toUpperCase() + '\n';

         //Efetua a primitiva send
         DataOutputStream saida = new DataOutputStream(conexao.getOutputStream());
         saida.writeBytes(str);

         conexao.close();
      }
   }
}
