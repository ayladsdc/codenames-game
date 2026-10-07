package objetos_comuns;

// Traduz os enums do jogo (CorCarta, Cargo) para o texto que vai na linha do protocolo
public class ConversorProtocolo {

    private ConversorProtocolo(){}

    // CorCarta (VERMELHA/AZUL) -> time do protocolo (VERMELHO/AZUL)
    public static String nomeTime(CorCarta time) {
        if (time == CorCarta.VERMELHA) return Protocolo.Time.VERMELHA;
        if (time == CorCarta.AZUL)     return Protocolo.Time.AZUL;
        throw new IllegalArgumentException("Time inválido: " + time);
    }

    // monta o nome do cargo do jeito do protocolo, com a cor junto
    public static String nomeCargo(Cargo cargo) {
        String tipo = cargo.eMestreEspiao() ? "MESTREESPIAO" : "AGENTE";
        return nomeTime(cargo.time()) + "_" + tipo;
    }

    public static String corParaProtocolo(CorCarta cor)     {return cor.name();}
    public static CorCarta corDoProtocolo(String valor)     {return CorCarta.valueOf(valor);}
}
