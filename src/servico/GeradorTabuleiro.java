package servico;

import objetos_comuns.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


public class GeradorTabuleiro {

    public List<String> scanearPacote(Scanner listaBruta){
        List<String> listaProcessada = new ArrayList<String>();

        while(listaBruta.hasNextLine()) listaProcessada.add(listaBruta.nextLine());

        return listaProcessada;
    }


    
    public List<Carta> embaralharLista(List<String> lista){

        int linhasTotais = lista.size();
        int fimLinhas = linhasTotais - 25; //pela randomização utilizada, será uma contagem decrescente do fim dela, até (ela - 25)

        List<Carta> listaCartas = new ArrayList<Carta>();
        Random random = new Random();

        for(int i = linhasTotais - 1; i >= fimLinhas; i--){
            int j = random.nextInt(i + 1);

            String temp = lista.get(i);
            lista.set(i, lista.get(j));
            lista.set(j, temp);

            listaCartas.add(new Carta(lista.get(i), CorCarta.NEUTRA)); 
            //TODO: coloquei neutra aqui pra só ir fznd o sorteio, mas precisa ver quantas vermelhas, azuis e tals para ser certinho
        }

        return listaCartas; 
    }

    public List<Carta> gerarTabuleiro(){
        InputStream pacotePalavras = GeradorTabuleiro.class.getResourceAsStream("/palavras.txt");
        Scanner lista = new Scanner(pacotePalavras, "UTF-8");
        List<String> listaProcessada = scanearPacote(lista);
        List<Carta> tabuleiro = embaralharLista(listaProcessada);
    
        return tabuleiro;
    }
}

