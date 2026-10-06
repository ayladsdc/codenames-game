package objetos_comuns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Mensagem {

    private final String comando;
    private final List<String> args;

    private Mensagem(String comando, List<String> args){
        this.comando = comando;
        this.args = List.copyOf(args); //list copy of para
    }

    //parse irá tratar o comando e fazer o construct por aqui
    public static Mensagem parse(String linha){
        if(linha == null || linha.isBlank()) return new Mensagem("", List.of());

        String comando;
        List<String> args;
        String linhaTrim = linha.trim();

        if(linhaTrim.startsWith(Protocolo.Servidor.INFO + Protocolo.SEPARADOR)){
            comando = Protocolo.Servidor.INFO;
            args = List.of((linhaTrim.substring(linhaTrim.indexOf(" ")).trim())); //trim para o caso "INFO         textinhoblablabla"
        } else {

            List<String> partes = Arrays.asList(linhaTrim.split("\\s+")); // "\\s+ é um ou mais espaços"

            comando = partes.get(0);
            args = partes.subList(1, partes.size());
        }

        return new Mensagem(comando, args);
    }

    public String paraLinha(){
        if (args.isEmpty()) return comando;
        String paralinha = this.comando + Protocolo.SEPARADOR + String.join(Protocolo.SEPARADOR, this.args);

        return paralinha;
    }

    public boolean vazia(){return comando.isBlank();}
    public String getComando() {return comando;}
    public List<String> getArgs() {return args;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Mensagem outra = (Mensagem) o;
        return comando.equals(outra.comando) && args.equals(outra.args);
    }

    @Override
    public int hashCode() {return Objects.hash(comando, args);} //essa função o Claude recomendou acrescentar por conta do Override do equals

    @Override
    public String toString(){return paraLinha();}


    private static Mensagem criar(String comando, List<String> args) {
        boolean textoLivre = comando.equals(Protocolo.Servidor.INFO); // único que pode ter espaço no argumento

        for (String a : args) {
            if (a == null || a.isEmpty()) {
                throw new IllegalArgumentException("Argumento vazio em " + comando);
            }
            if (a.contains("\n") || a.contains("\r")) { // quebra de linha viraria DUAS mensagens
                throw new IllegalArgumentException("Argumento com quebra de linha em " + comando);
            }
            if (!textoLivre && a.contains(" ")) {
                throw new IllegalArgumentException("Argumento com espaço em " + comando + ": '" + a + "'");
            }
        }
        return new Mensagem(comando, args);
    }

    // INFO é o único com texto livre: o texto inteiro vai como UM argumento só
    public static Mensagem info(String texto) {
        if (texto == null) throw new IllegalArgumentException("Texto do INFO nulo");

        String limpo = texto.trim();
        if (limpo.isEmpty()) return criar(Protocolo.Servidor.INFO, List.of());

        return criar(Protocolo.Servidor.INFO, List.of(limpo));
    }

    public static Mensagem tabuleiro(Tabuleiro tabuleiro, boolean visaoMestre) {
        String comando = visaoMestre ? Protocolo.Servidor.TABULEIRO_MESTRE : Protocolo.Servidor.TABULEIRO_AGENTE;
        return criar(comando, CodificadorTabuleiro.codificar(tabuleiro, visaoMestre));
    }

    // no fim do jogo todo mundo recebe no formato do mestre
    public static Mensagem tabuleiroFinal(Tabuleiro tabuleiro) {
        return criar(Protocolo.Servidor.TABULEIRO_FINAL, CodificadorTabuleiro.codificar(tabuleiro, true));
    }

    // a mesma coisa, mas já devolvendo a linha pronta (como pede a issue)
    public static String codificarTabuleiro(Tabuleiro tabuleiro, boolean visaoMestre) {
        return tabuleiro(tabuleiro, visaoMestre).paraLinha();
    }

    // Recebe a linha inteira (com o comando). Se estiver malformada, lança IllegalArgumentException.
    public static List<CartaVisivel> decodificarTabuleiro(String linha) {
        Mensagem m = parse(linha);
        String comando = m.getComando();

        boolean ehTabuleiro = comando.equals(Protocolo.Servidor.TABULEIRO_AGENTE) || comando.equals(Protocolo.Servidor.TABULEIRO_MESTRE) || comando.equals(Protocolo.Servidor.TABULEIRO_FINAL);
        if (!ehTabuleiro) {throw new IllegalArgumentException("Não é uma mensagem de tabuleiro: '" + comando + "'");}

        return CodificadorTabuleiro.decodificar(m.getArgs());
    }

    private static Mensagem criarMensagem(String comando, String... args) {return criar(comando, Arrays.asList(args));}

    //mensagens do cliente
    public static Mensagem cargo(Cargo cargo)                   {return criarMensagem(Protocolo.Cliente.CARGO, ConversorProtocolo.nomeCargo(cargo));}
    public static Mensagem dica(String palavra, int numero)     {return criarMensagem(Protocolo.Cliente.DICA, palavra, String.valueOf(numero));}
    public static Mensagem chute(int posicao)                   {return criarMensagem(Protocolo.Cliente.CHUTE, String.valueOf(posicao));}
    public static Mensagem passa()                              {return criarMensagem(Protocolo.Cliente.PASSA);}

    //JOGO <estado>
    public static Mensagem jogo(String estado)                  {return criarMensagem(Protocolo.Servidor.JOGO, estado);}
    public static Mensagem bemVindo(Cargo cargo)                {return criarMensagem(Protocolo.Servidor.JOGO, Protocolo.Jogo.BEM_VINDO, ConversorProtocolo.nomeCargo(cargo));}

    public static Mensagem jogoIniciado()  { return jogo(Protocolo.Jogo.INICIADO); }
    public static Mensagem jogoEncerrado() { return jogo(Protocolo.Jogo.ENCERRADO); }
    public static Mensagem dicaValida()    { return jogo(Protocolo.Jogo.DICA_VALIDA); }
    public static Mensagem chuteValido()   { return jogo(Protocolo.Jogo.CHUTE_VALIDO); }
    public static Mensagem passaValida()   { return jogo(Protocolo.Jogo.PASSA_VALIDA); }

    public static Mensagem cargosLivres(List<Cargo> livres) {
        List<String> nomes = new ArrayList<>();
        for (Cargo c : livres) nomes.add(ConversorProtocolo.nomeCargo(c));
        return criar(Protocolo.Servidor.CARGOS_LIVRES, nomes);
    }

    //retorno da mensagens
    public static Mensagem placar           (int vermelhoRestantes, int azulRestantes) {return criarMensagem(Protocolo.Servidor.PLACAR, String.valueOf(vermelhoRestantes), String.valueOf(azulRestantes));}
    public static Mensagem vezDica          (CorCarta time) {return criarMensagem(Protocolo.Servidor.VEZ_DICA, ConversorProtocolo.nomeTime(time));}
    public static Mensagem dicaDada         (String palavra, int numero) {return criarMensagem(Protocolo.Servidor.DICA_DADA, palavra, String.valueOf(numero));}
    public static Mensagem vezPalpite       (CorCarta time, int palpitesRestantes) {return criarMensagem(Protocolo.Servidor.VEZ_PALPITE, ConversorProtocolo.nomeTime(time), String.valueOf(palpitesRestantes));}
    public static Mensagem revelar          (int posicao, CorCarta cor) {return criarMensagem(Protocolo.Servidor.REVELAR, String.valueOf(posicao), ConversorProtocolo.corParaProtocolo(cor));}
    public static Mensagem fimTurno         (String motivo, CorCarta proximoTime) {return criarMensagem(Protocolo.Servidor.FIM_TURNO, motivo, ConversorProtocolo.nomeTime(proximoTime));}
    public static Mensagem vencedor         (CorCarta time, String motivo) {return criarMensagem(Protocolo.Servidor.VENCEDOR, ConversorProtocolo.nomeTime(time), motivo);}
    public static Mensagem erro             (String motivo) {return criarMensagem(Protocolo.Servidor.ERRO, motivo);}
    public static Mensagem erroCargoOcupado (Cargo cargo) {return criarMensagem(Protocolo.Servidor.ERRO, Protocolo.Erro.CARGO_OCUPADO, ConversorProtocolo.nomeCargo(cargo));}
}
