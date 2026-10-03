package objetos_comuns;


//Por conta desse arquivo ser totalmente baseado no PROTOCOLO.md e algo bastante repetitivo, 
//foi utilizado o Claude para gerar esse arquivo com base no que escrevemos originalmente.

/*Aqui ficam os "comandos"/palavras que ditam o que aquela mensagem vai ser
* para o programa conseguir interpretar o que vai ter que fazer.
* A especificação completa está em docs/PROTOCOLO.md (é a fonte da verdade).
*
* Cada mensagem é uma linha UTF-8 (terminada em \n) no formato:
*   COMANDO argumento1 argumento2 ...
* Palavras com espaço usam '_' no lugar do espaço (ex.: JET_SKI).
*
* Mensagens Cliente -> Servidor
*   CARGO <NOME_DO_CARGO>             Jogador escolhe o cargo no lobby
*   DICA <palavra> <numero>           Mestre espião da vez envia a dica (numero de 1 a 9)
*   CHUTE <posicao>                   Agente da vez chuta uma carta (posicao de 1 a 25)
*   PASSA                             Agente da vez passa a vez para o outro time
*
* Mensagens Servidor -> Cliente
*   JOGO <estado>                     Respostas de sucesso e marcos do jogo (ver Jogo)
*   CARGOS_LIVRES <cargo> ...         Cargos ainda livres no lobby
*   TABULEIRO_AGENTE <carta> x25      Tabuleiro sem as cores das cartas ocultas
*   TABULEIRO_MESTRE <carta> x25      Tabuleiro com todas as cores
*   TABULEIRO_FINAL <carta> x25       Tabuleiro completo, enviado a todos no fim do jogo
*                                     (carta = <posicao>:<palavra>:<cor>:<revelada>)
*   PLACAR <vermelho> <azul>          Cartas de cada time que ainda não foram reveladas
*   VEZ_DICA <time>                   Começa a fase de dica do time
*   DICA_DADA <palavra> <numero>      Dica aceita, repassada a todos
*   VEZ_PALPITE <time> <restantes>    Começa (ou continua) a fase de palpite do time
*   REVELAR <posicao> <cor>           Carta revelada depois de um chute válido
*   FIM_TURNO <motivo> <proximo_time> Encerra o turno
*   VENCEDOR <time> <motivo>          Anuncia o time vencedor
*   INFO <texto livre>                Aviso para exibir (único com espaços no texto)
*   ERRO <motivo> [cargo]             Erro, enviado só a quem errou ('cargo_ocupado' leva o cargo)
*/

public class Protocolo {

    private Protocolo(){}

    public static final int PORTA_PADRAO = 1996;
    public static final int TAMANHO_MAXIMO_LINHA = 2048;

    public static final String SEPARADOR = " ";               // entre comando e argumentos
    public static final String SEPARADOR_CAMPOS_CARTA = ":";  // dentro de uma carta do tabuleiro
    public static final String CARTA_OCULTA = "?";            // no lugar da cor, no TABULEIRO_AGENTE

    public static class Cliente {
        public static final String CARGO = "CARGO";
        public static final String DICA  = "DICA";
        public static final String CHUTE = "CHUTE";
        public static final String PASSA = "PASSA";
    }

    public static class Servidor {
        public static final String INFO = "INFO";
        public static final String JOGO = "JOGO";
        public static final String CARGOS_LIVRES = "CARGOS_LIVRES";
        public static final String TABULEIRO_AGENTE = "TABULEIRO_AGENTE";
        public static final String TABULEIRO_MESTRE = "TABULEIRO_MESTRE";
        public static final String TABULEIRO_FINAL = "TABULEIRO_FINAL";
        public static final String PLACAR = "PLACAR";
        public static final String VEZ_DICA = "VEZ_DICA";
        public static final String DICA_DADA = "DICA_DADA";
        public static final String VEZ_PALPITE = "VEZ_PALPITE";
        public static final String REVELAR = "REVELAR";
        public static final String FIM_TURNO = "FIM_TURNO";
        public static final String VENCEDOR = "VENCEDOR";
        public static final String ERRO = "ERRO";
    }

    // Segundo termo da mensagem 'JOGO <estado>'
    public static class Jogo {
        public static final String BEM_VINDO = "bem_vindo";        // sucesso de CARGO (JOGO bem_vindo <cargo>)
        public static final String INICIADO = "iniciado";
        public static final String ENCERRADO = "encerrado";
        public static final String DICA_VALIDA = "dica_valida";    // sucesso de DICA
        public static final String CHUTE_VALIDO = "chute_valido";  // sucesso de CHUTE
        public static final String PASSA_VALIDA = "passa_valida";  // sucesso de PASSA
    }

    // Motivo de 'ERRO <motivo>'
    public static class Erro {
        public static final String PARTIDA_CHEIA = "partida_cheia";
        public static final String LINHA_VAZIA = "linha_vazia";
        public static final String LINHA_LONGA = "linha_longa";
        public static final String COMANDO_DESCONHECIDO = "comando_desconhecido";
        public static final String ARGUMENTOS_INVALIDOS = "argumentos_invalidos";
        public static final String CARGO_INVALIDO = "cargo_invalido";
        public static final String CARGO_OCUPADO = "cargo_ocupado";       // ERRO cargo_ocupado <cargo>
        public static final String CARGO_JA_ESCOLHIDO = "cargo_ja_escolhido";
        public static final String FORA_DE_HORA = "fora_de_hora";
        public static final String FORA_DE_VEZ = "fora_de_vez";
        public static final String PAPEL_INVALIDO = "papel_invalido";
        public static final String POSICAO_INVALIDA = "posicao_invalida";
        public static final String CARTA_JA_REVELADA = "carta_ja_revelada";
        public static final String DICA_INVALIDA = "dica_invalida";
        public static final String NUMERO_INVALIDO = "numero_invalido";
    }

    // Motivo de 'FIM_TURNO <motivo> <proximo_time>'
    public static class MotivoFimTurno {
        public static final String ERROU = "errou";
        public static final String PASSOU = "passou";
        public static final String SEM_PALPITES = "sem_palpites";
    }

    // Motivo de 'VENCEDOR <time> <motivo>'
    public static class MotivoVencedor {
        public static final String TODAS_CARTAS = "todas_cartas";
        public static final String ASSASSINO = "assassino";
    }

    // Cor da carta como aparece nas mensagens (o enum CorCarta usa VERMELHO e ASSASSINO,
    // a conversão entre os dois fica na classe Mensagem)
    public static class Cor {
        public static final String VERMELHA = "VERMELHA";
        public static final String AZUL = "AZUL";
        public static final String NEUTRA = "NEUTRA";
        public static final String ASSASSINA = "ASSASSINA";
    }

    // Time como aparece nas mensagens (mesmo prefixo dos cargos)
    public static class Time {
        public static final String VERMELHO = "VERMELHO";
        public static final String AZUL = "AZUL";
    }
}