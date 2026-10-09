package logica_jogo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**  o resultado é consummido pelo servidro  e é literalmente o resultado de uma ação. 
 * Se a jogada foi inválida, traz só o código de erro do protocolo. Se foi válida, traz
 *  a lista de eventos que ocorreram (ex.: carta revelada, depois fim de turno).*/
public class Resultado {
    private final boolean sucesso;
    private final String erro;
    private final List<Evento> eventos; 
    
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
