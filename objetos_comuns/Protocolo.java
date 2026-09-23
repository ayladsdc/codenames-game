package objetos_comuns;

/*Aqui ficam os "comandos"/palavras que ditam o que aquela mensagem vai ser
* pra o programa conseguir intepretar o que vai ter que fazer 
* Cada mensagem é uma linha (terminada em \n) no formato:
*   COMANDO argumento1 argumento2 ...
*
* Mensagens Cliente -> Servidor
*   CARGO <nome_cargo>                Jogadores conectados escolhem o cargo
*   DICA <palavra_dica> <numero>      Mestre espião envia dica e numero de palavras que afeta
*   CHUTE <palavra_carta>             Agente tenta adivinhar a palavra   
*   PASSA                             Jogador passa a rodada para o outro time   

* Mensagens Servidor -> Cliente
*/

public class Protocolo {

    private Protocolo(){}

    public static class Client {
        public static final String CARGO = "CARGO";
        public static final String DICA  = "DICA";
        public static final String CHUTE = "CHUTE";
        public static final String PASSA = "PASSA";
    }  

    public static class Servidor {
        
    }
}
