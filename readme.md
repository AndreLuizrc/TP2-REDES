# 🎮 Battle Arena - Multiplayer TCP/UDP

Este é um jogo multiplayer em tempo real desenvolvido em Java,
demonstrando uma arquitetura de redes híbrida utilizando Sockets **TCP**
e **UDP**. O projeto simula uma arena de combate 1v1 com mecânicas de
tiro "Shotgun" (espalhado), obstáculos e física de projéteis
independente.

## 📋 Pré-requisitos

Para rodar este projeto, certifique-se de ter instalado em sua máquina:

-   **Java JDK** (versão 8 ou superior)
-   **Apache Maven** (para gerenciamento de dependências e build)
-   **VS Code** (ou qualquer IDE Java de sua preferência)

## 🚀 Como Compilar e Rodar

Como este é um sistema distribuído (Cliente-Servidor), você precisará
abrir **3 terminais** simultaneamente para simular o ambiente.

### 1. Compilar o Projeto

Abra um terminal na pasta raiz do projeto (onde está o arquivo
`pom.xml`) e execute o comando para limpar builds antigos e compilar o
código novo:

``` bash
mvn clean compile
```

### 2. Iniciar o Servidor (O Host)

No Terminal 1, inicie o servidor. Ele será responsável por gerenciar o
mapa, a física dos tiros e a sincronização entre jogadores.

``` bash
mvn exec:java -Dexec.mainClass="com.mycompany.app.App"
```

Você verá mensagens indicando que as portas TCP e UDP foram abertas.

### 3. Iniciar o Jogador 1

No Terminal 2, inicie o primeiro cliente. Uma janela gráfica preta será
aberta.

``` bash
mvn exec:java -Dexec.mainClass="com.mycompany.app.Cliente"
```

### 4. Iniciar o Jogador 2

No Terminal 3, inicie o segundo cliente. Uma segunda janela será aberta.

``` bash
mvn exec:java -Dexec.mainClass="com.mycompany.app.Cliente"
```

## 🕹️ Controles e Regras

**Importante:** Certifique-se de clicar dentro da janela do jogo para
que ela capture os comandos do teclado.

  Tecla    Ação
  -------- ---------------------
  W        Mover para Cima
  S        Mover para Baixo
  A        Mover para Esquerda
  D        Mover para Direita
  ESPAÇO   Atirar (Shotgun)

### Mecânicas de Jogo

-   **Tiro Espalhado (Shotgun):** Ao atirar, o jogador dispara 3
    projéteis (um reto e dois nas diagonais).
-   **Cobertura:** Paredes (#) bloqueiam tanto a movimentação quanto os
    tiros.
-   **Imunidade:** Você não sofre dano dos seus próprios tiros, apenas
    dos tiros do inimigo.
-   **Cooldown:** Existe um tempo de recarga de 2 segundos entre os
    disparos.

## ⚙️ Arquitetura Técnica

Este projeto foi desenhado para ilustrar as diferenças práticas entre
protocolos de transporte:

### Protocolo TCP (Porta 6789)

**Uso:** Handshake e Identificação (Login).\
**Por que TCP?** A atribuição de ID (saber se sou o Jogador 1 ou 2) é
crítica e não pode haver perda de dados. O TCP garante a entrega dessa
informação na inicialização.

### Protocolo UDP (Porta 9876)

**Uso:** Movimentação, Tiros e Atualização de Tela.\
**Por que UDP?** Jogos em tempo real exigem baixa latência. Se um pacote
de posição for perdido, é melhor receber logo o próximo (posição
atualizada) do que travar o jogo esperando retransmissão (como faria o
TCP).

### Multithreading no Servidor

O servidor executa 3 threads principais simultaneamente:

-   **Thread TCP:** Aguarda novas conexões de login.
-   **Thread UDP:** Processa comandos de input (W, A, S, D, SHOOT) dos
    clientes.
-   **Game Loop (Physics):** Uma thread autônoma que roda a cada 333ms
    para calcular a posição das balas e verificar colisões, independente
    da ação dos usuários.

## 🛠️ Solução de Problemas Comuns

### Erro `Address already in use`:

Isso significa que você tentou abrir o servidor duas vezes ou o processo
antigo não foi fechado corretamente.\
**Solução:** Feche todos os terminais ou mate os processos `java` no
gerenciador de tarefas e tente novamente.

### Comandos não respondem:

Verifique se a janela do jogo (Java App) está focada (selecionada). O
terminal às vezes "rouba" o foco do teclado.

### Portas Bloqueadas:

Se estiver rodando em máquinas diferentes na mesma rede, certifique-se
de que o Firewall do Windows/Linux permite tráfego nas portas **6789** e
**9876**.
