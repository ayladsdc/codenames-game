# Codenames (Redes de Computadores)

Réplica do jogo de tabuleiro **Codenames** em Java, com arquitetura cliente-servidor sobre **sockets TCP**. Uma partida tem 4 jogadores: um **mestre espião** e um **agente** em cada time (vermelho e azul).

## Pré-requisitos

- **JDK 17 ou superior** (testado com o 21). É preciso o JDK, não só o JRE: confira com `javac -version`.
- Nenhuma biblioteca externa.

## Como compilar

Linux / macOS / WSL / Git Bash:

```bash
./build.sh
```

Windows (cmd ou PowerShell):

```bat
build.bat
```

Isso compila tudo que está em `src/` e `resources/` para a pasta `out/` (ignorada pelo Git).

## Como rodar

Sempre a partir da **raiz do repositório**, porque o servidor lê `resources/palavras.txt` pelo classpath e a raiz precisa estar nele.

### 1. Servidor

```bash
./run-servidor.sh          # porta padrão: 1996
./run-servidor.sh 2000     # ou outra porta
```

No Windows: `run-servidor.bat` (ou `run-servidor.bat 2000`).

Ele deve imprimir `Aguardando 4 jogadores...` e ficar esperando as conexões.

### 2. Os 4 clientes

Abra **4 terminais** (um por jogador), na raiz do repositório, e rode em cada um:

```bash
./run-cliente.sh           # Windows: run-cliente.bat
```

Cada jogador escolhe um dos cargos livres que o servidor listar (mestre espião e agente de cada time). Quando os 4 cargos estiverem ocupados, a partida começa.

### Rodando sem os scripts

```bash
java -cp out:. servidor.ServidorCodenames      # Windows: java -cp out;. servidor.ServidorCodenames
java -cp out:. cliente.ClienteCodenames        # Windows: java -cp out;. cliente.ClienteCodenames
```

## Protocolo

As mensagens trocadas entre cliente e servidor (formato, comandos, erros e fluxo da partida) estão em [docs/PROTOCOLO.md](docs/PROTOCOLO.md). Esse arquivo é a fonte da verdade.

## Estrutura

```
src/
  servidor/        servidor TCP e conexão de cada jogador
  cliente/         cliente do jogador
  logica_jogo/     regras e estados da partida
  objetos_comuns/  Tabuleiro, Carta, Cargo, Protocolo...
  servico/         geração do tabuleiro
resources/         lista de palavras (palavras.txt)
docs/              documentação (PROTOCOLO.md)
```