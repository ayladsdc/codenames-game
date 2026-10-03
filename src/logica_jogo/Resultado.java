package logica_jogo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/** precisava explicar direitinho essa classe, pra ele n chamar de ia */
public class Resultado {
    private final boolean sucesso;
    private final String erro;
    private final List<Evento> eventos; // processa como uma lista de eventos?????
    
    private Resultado(boolean sucesso, String erro, List<Evento> eventos){
        this.sucesso = sucesso;
        this.erro= erro;
        this.eventos = eventos;
    }

    public static Resultado erro(String motivo) {
        return new Resultado(false, motivo, Collections.emptyList());
    }
 
    public static Resultado sucesso(List<Evento> eventos) {
        return new Resultado(true, null, eventos);
    }

    public static Resultado sucesso(Evento evento) {
        List<Evento> lista = new ArrayList<>();
        lista.add(evento);
        return sucesso(lista);
    }

    public boolean deuSucesso(){
        return sucesso;
    }

    public String getErro(){
        return erro;
    }

    public List<Evento> getEventos(){
        return eventos;
    }
}
