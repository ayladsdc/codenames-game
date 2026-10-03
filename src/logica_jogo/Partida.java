package logica_jogo;

import java.util.ArrayList;
import java.util.List;

import objetos_comuns.Cargo;
import objetos_comuns.Carta;
import objetos_comuns.CorCarta;
import objetos_comuns.Tabuleiro;

/** A intenção disso tudo é fazer com que a a Partida controle toda a lógica do jogo, sendo ativada pelo servidor, que 
* lida com o cliente/sockets. Ela retorna um "Resultado", que é digerido pelo servidor.
*/

public class Partida {
     private final Tabuleiro tabuleiro;
    private CorCarta timeDaVez;
    private Fase fase;
    private int palpitesRestantes;
    private CorCarta vencedor;
    private String motivoFim;
 
    public Partida(Tabuleiro tabuleiro) {
        this.tabuleiro = tabuleiro;
        this.timeDaVez = tabuleiro.getTimeInicio();
        this.fase = Fase.AGUARDANDO_DICA;
        this.palpitesRestantes = 0;
        this.vencedor = null;
        this.motivoFim = null;
    }

    public CorCarta getTimeDaVez() {
        return timeDaVez;
    }
 
    public Fase getFase() {
        return fase;
    }
 
    public CorCarta getVencedor() {
        return vencedor;
    }
 
    public String getMotivoFim() {
        return motivoFim;
    }
 
    public Tabuleiro getTabuleiro() {
        return tabuleiro;
    }
 
    public int getPalpitesRestantes() {
        return palpitesRestantes;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Aqui vão ficar todas as ações que um jogador pode fazer, com todas suas exeções

    public Resultado darDica(Cargo jogador, String palavra, int numero){
        //primeiro, trata todos os possiveis erros
        if(fase!=Fase.AGUARDANDO_DICA){
            return Resultado.erro("Não é hora de dar dicas!!");
        }
        else if(!jogador.eMestreEspiao()){
            return Resultado.erro("Você não da dica não amore");
        }
        else if(timeDaVez !=jogador.time()){
            return Resultado.erro("Não é nem sua vez de jogar >:(");
        }

        // aqui ele ve o numero da palpites é valido: maior ou igual a 1 E menor ou igual ao que o numero de cartas restantes
        if(numero < 1){ 
            return Resultado.erro("O numero de palpites tem que ser maior ou igual a 1 ");
        } else if(numero > tabuleiro.cartasRestantes(jogador.time())){
            return Resultado.erro("O numero de palpites tem que ser menor do que o número de cartas restantes da cor " + jogador.time().name());
        }
        
        return Resultado.erro("eh só pra a função n reclamar, isso é um teste");
        //num <1
        //palavra nula?
        // n eh o time da vez
        // a dicaeh uma palavra  do tabuleiro
    }

    // public Resultado chutar(){
          
    // }

    public static void main(String[] args) {
        Tabuleiro tabuleiro = new Tabuleiro();
        Partida partida = new Partida(tabuleiro);
        Cargo cara; 
        //Cargo cara = Cargo.AZUL_MESTREESPIAO;
        if(tabuleiro.getTimeInicio()==CorCarta.AZUL)
            cara = Cargo.AZUL_MESTREESPIAO;
        else
            cara = Cargo.VERMELHO_MESTREESPIAO;

        Resultado r1 = partida.darDica(cara, "animal", 10);
        System.out.println(r1.deuSucesso() + " | fase=" + partida.getFase() + " " + r1.getErro());
    }
}
