package cliente;

import objetos_comuns.Protocolo;


/** Classe responsavel por ler as mensagens escritas com os "protocolos" e traduzilas para linguagem natural */
public class Tradutor {

    public static String paraCliente(String linha) {
        String[] partes = linha.split(Protocolo.SEPARADOR);
        String comando = partes[0];

        if (comando.equals(Protocolo.Servidor.INFO)) {
            return linha.substring(comando.length() + 1); // o resto da linha, já é texto livre
        }
        if (comando.equals(Protocolo.Servidor.JOGO)) {
            return traduzirJogo(partes);
        }
        if (comando.equals(Protocolo.Servidor.ERRO)) {
            return "Erro: " + traduzirErro(partes);
        }
        if (comando.equals(Protocolo.Servidor.CARGOS_LIVRES)) {
            return "Cargos disponíveis: " + String.join(", ", semPrimeiro(partes)) + " (Use: CARGO <NOME>)";
        }
        if (comando.equals(Protocolo.Servidor.TABULEIRO_AGENTE)
                || comando.equals(Protocolo.Servidor.TABULEIRO_MESTRE)
                || comando.equals(Protocolo.Servidor.TABULEIRO_FINAL)) {
            return "Tabuleiro:" + renderizarTabuleiro(partes);
        }
        if (comando.equals(Protocolo.Servidor.PLACAR)) {
            return "Cartas restantes -> Vermelho: " + partes[1] + " | Azul: " + partes[2];
        }
        if (comando.equals(Protocolo.Servidor.VEZ_DICA)) {
            return "É a vez do time " + partes[1] + " dar a dica. (Use: DICA <palavra> <numero>)";
        }
        if (comando.equals(Protocolo.Servidor.DICA_DADA)) {
            return "Dica: '" + partes[1] + "' (" + partes[2] + " palavras)";
        }
        if (comando.equals(Protocolo.Servidor.VEZ_PALPITE)) {
            return "É a vez do time " + partes[1] + " chutar (" + partes[2]
                    + " tentativa(s) restante(s)). (Use: CHUTE <posicao> ou PASSA)";
        }
        if (comando.equals(Protocolo.Servidor.REVELAR)) {
            return "A carta da posição " + partes[1] + " era " + partes[2].toLowerCase() + "!";
        }
        if (comando.equals(Protocolo.Servidor.FIM_TURNO)) {
            return "Fim de turno (" + traduzirMotivoFimTurno(partes[1]) + "). Agora é a vez do time " + partes[2] + ".";
        }
        if (comando.equals(Protocolo.Servidor.VENCEDOR)) {
            return "O time " + partes[1] + " venceu! (" + traduzirMotivoVencedor(partes[2]) + ")";
        }

        return linha; // comando que não reconhecemos, mostra cru mesmo
    }

    /** Desenha o tabuleiro recebido (TABULEIRO_AGENTE/MESTRE/FINAL), 5 cartas por linha. */
    private static String renderizarTabuleiro(String[] partes) {
        StringBuilder sb = new StringBuilder("\n");
        int coluna = 0;

        for (int i = 1; i < partes.length; i++) {
            String[] campos = partes[i].split(Protocolo.SEPARADOR_CAMPOS_CARTA);
            String posicao = campos[0];
            String palavra = campos[1].replace("_", " ");
            String cor = campos[2];
            boolean revelada = campos[3].equals("1");

            String marcador = revelada ? "*" : " ";
            String texto = "[" + posicao + "]" + marcador + palavra;
            if (!cor.equals(Protocolo.CARTA_OCULTA)) {
                texto += "(" + cor.toLowerCase() + ")";
            }

            sb.append(String.format("%-26s", texto));
            coluna++;
            if (coluna % 5 == 0) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static String traduzirJogo(String[] partes) {
        String estado = partes[1];

        if (estado.equals(Protocolo.Jogo.BEM_VINDO)) {
            return "Você entrou como " + partes[2] + ".";
        }
        if (estado.equals(Protocolo.Jogo.INICIADO)) {
            return "A partida começou!";
        }
        if (estado.equals(Protocolo.Jogo.ENCERRADO)) {
            return "A partida acabou.";
        }
        if (estado.equals(Protocolo.Jogo.DICA_VALIDA)) {
            return "Dica enviada.";
        }
        if (estado.equals(Protocolo.Jogo.CHUTE_VALIDO)) {
            return "Chute registrado.";
        }
        if (estado.equals(Protocolo.Jogo.PASSA_VALIDA)) {
            return "Você passou a vez.";
        }
        return estado;
    }

    private static String traduzirErro(String[] partes) {
        String motivo = partes[1];

        if (motivo.equals(Protocolo.Erro.PARTIDA_CHEIA)) return "a partida já está cheia";
        if (motivo.equals(Protocolo.Erro.LINHA_VAZIA)) return "você enviou uma linha vazia";
        if (motivo.equals(Protocolo.Erro.LINHA_LONGA)) return "essa mensagem é longa demais";
        if (motivo.equals(Protocolo.Erro.COMANDO_DESCONHECIDO)) return "comando desconhecido";
        if (motivo.equals(Protocolo.Erro.ARGUMENTOS_INVALIDOS)) return "argumentos inválidos";
        if (motivo.equals(Protocolo.Erro.CARGO_INVALIDO)) return "esse cargo não existe";
        if (motivo.equals(Protocolo.Erro.CARGO_OCUPADO)) return "o cargo " + partes[2] + " já está ocupado";
        if (motivo.equals(Protocolo.Erro.CARGO_JA_ESCOLHIDO)) return "você já escolheu um cargo";
        if (motivo.equals(Protocolo.Erro.FORA_DE_HORA)) return "o jogo ainda não começou";
        if (motivo.equals(Protocolo.Erro.FORA_DE_VEZ)) return "não é a sua vez";
        if (motivo.equals(Protocolo.Erro.PAPEL_INVALIDO)) return "seu papel não pode fazer isso agora";
        if (motivo.equals(Protocolo.Erro.POSICAO_INVALIDA)) return "essa posição não existe";
        if (motivo.equals(Protocolo.Erro.CARTA_JA_REVELADA)) return "essa carta já foi revelada";
        if (motivo.equals(Protocolo.Erro.DICA_INVALIDA)) return "essa dica não é permitida";
        if (motivo.equals(Protocolo.Erro.NUMERO_INVALIDO)) return "número inválido";
        return motivo;
    }

    private static String traduzirMotivoFimTurno(String motivo) {
        if (motivo.equals(Protocolo.MotivoFimTurno.ERROU)) return "o time errou o palpite";
        if (motivo.equals(Protocolo.MotivoFimTurno.PASSOU)) return "o time passou a vez";
        if (motivo.equals(Protocolo.MotivoFimTurno.SEM_PALPITES)) return "acabaram as tentativas";
        return motivo;
    }

    private static String traduzirMotivoVencedor(String motivo) {
        if (motivo.equals(Protocolo.MotivoVencedor.TODAS_CARTAS)) return "revelou todas as próprias cartas";
        if (motivo.equals(Protocolo.MotivoVencedor.ASSASSINA)) return "o outro time acertou o assassino";
        return motivo;
    }

    private static String[] semPrimeiro(String[] partes) {
        String[] resto = new String[partes.length - 1];
        System.arraycopy(partes, 1, resto, 0, resto.length);
        return resto;
    }

}