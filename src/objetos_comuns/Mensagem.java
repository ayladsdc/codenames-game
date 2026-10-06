package objetos_comuns;
import java.util.ArrayList; import java.util.Arrays; import java.util.List; import java.util.Objects;

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

        String comando; List<String> args;
        String linhaTrim = linha.trim();

        String[] partes = linhaTrim.split("\\s+", 2);
        comando = partes[0];

        if(partes.length == 1){
            args = List.of(); // chegou só o comando, sem argumentos (ex: "PASSA")
        } else if(comando.equals(Protocolo.Servidor.INFO)){
            args = List.of(partes[1]); // INFO: tudo depois do comando é UM argumento só (os espaços de dentro ficam)
        } else {
            args = Arrays.asList(partes[1].split("\\s+")); // "\\s+ é um ou mais espaços"
        }

        //PARA TESTAR: System.out.println(comando);
        //PARA TESTAR: System.out.println(args);

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

    //essa função o Claude recomendou acrescentar por conta do Override do equals  
    @Override
    public int hashCode() {return Objects.hash(comando, args);}

    @Override
    public String toString(){return paraLinha();}



    private static String nomeTime(CorCarta time) {
        if (time == CorCarta.VERMELHA) return Protocolo.Time.VERMELHA;
        if (time == CorCarta.AZUL)     return Protocolo.Time.AZUL;
        throw new IllegalArgumentException("Time inválido: " + time);
    }

    private static String nomeCargo(Cargo cargo)            {return nomeTime(cargo.time()) + "_" + (cargo.eMestreEspiao() ? "MESTREESPIAO" : "AGENTE");}


    //retorno da mensagens
    private static Mensagem criarMensagem(String comando, String... args) {return new Mensagem(comando, List.of(args));}
    public static Mensagem placar           (int vermelhoRestantes, int azulRestantes) {return criarMensagem(Protocolo.Servidor.PLACAR, String.valueOf(vermelhoRestantes), String.valueOf(azulRestantes));}
    public static Mensagem vezDica          (CorCarta time) {return criarMensagem(Protocolo.Servidor.VEZ_DICA, nomeTime(time));}
    public static Mensagem dicaDada         (String palavra, int numero) {return criarMensagem(Protocolo.Servidor.DICA_DADA, palavra, String.valueOf(numero));}
    public static Mensagem vezPalpite       (CorCarta time, int palpitesRestantes) {return criarMensagem(Protocolo.Servidor.VEZ_PALPITE, nomeTime(time), String.valueOf(palpitesRestantes));}
    public static Mensagem revelar          (int posicao, CorCarta cor) {return criarMensagem(Protocolo.Servidor.REVELAR, String.valueOf(posicao), cor.name());}
    public static Mensagem fimTurno         (String motivo, CorCarta proximoTime) {return criarMensagem(Protocolo.Servidor.FIM_TURNO, motivo, nomeTime(proximoTime));}
    public static Mensagem vencedor         (CorCarta time, String motivo) {return criarMensagem(Protocolo.Servidor.VENCEDOR, nomeTime(time), motivo);}
    public static Mensagem info             (String texto) {return criarMensagem(Protocolo.Servidor.INFO, texto.trim());}
    public static Mensagem erro             (String motivo) {return criarMensagem(Protocolo.Servidor.ERRO, motivo);}
    public static Mensagem erroCargoOcupado (Cargo cargo) {return criarMensagem(Protocolo.Servidor.ERRO, Protocolo.Erro.CARGO_OCUPADO, nomeCargo(cargo));}
    public static Mensagem cargo(Cargo cargo)               {return criarMensagem(Protocolo.Cliente.CARGO, nomeCargo(cargo));}
    public static Mensagem dica(String palavra, int numero) {return criarMensagem(Protocolo.Cliente.DICA, palavra, String.valueOf(numero));}
    public static Mensagem chute(int posicao)               {return criarMensagem(Protocolo.Cliente.CHUTE, String.valueOf(posicao));}
    public static Mensagem passa()                          {return criarMensagem(Protocolo.Cliente.PASSA);}
    public static Mensagem jogo(String estado)              {return criarMensagem(Protocolo.Servidor.JOGO, estado);}
    public static Mensagem bemVindo(Cargo cargo)            {return criarMensagem(Protocolo.Servidor.JOGO, Protocolo.Jogo.BEM_VINDO, nomeCargo(cargo));}
    public static Mensagem jogoIniciado()                   {return jogo(Protocolo.Jogo.INICIADO); }
    public static Mensagem jogoEncerrado()                  {return jogo(Protocolo.Jogo.ENCERRADO); }
    public static Mensagem dicaValida()                     {return jogo(Protocolo.Jogo.DICA_VALIDA); }
    public static Mensagem chuteValido()                    {return jogo(Protocolo.Jogo.CHUTE_VALIDO); }
    public static Mensagem passaValida()                    {return jogo(Protocolo.Jogo.PASSA_VALIDA); }

    public static Mensagem cargosLivres(List<Cargo> livres) {
        List<String> nomes = new ArrayList<>();
        for (Cargo c : livres) nomes.add(nomeCargo(c));
        return new Mensagem(Protocolo.Servidor.CARGOS_LIVRES, nomes);
    }

    /*
    //Main para testar os outputs  
    public static void main(String[] args) {
        String testes = "INFO    Aguardando   jogadores";

        Mensagem m = parse(testes);
        Mensagem.parse(m.paraLinha());
    }

     */
}
