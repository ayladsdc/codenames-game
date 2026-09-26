package servico;

import objetos_comuns.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


public class GeradorTabuleiro {

    public List<String> scanearPacote(Scanner listaBruta){
        List<String> listaProcessada = new ArrayList<String>();

        while(listaBruta.hasNextLine()) listaProcessada.add(listaBruta.nextLine());

        return listaProcessada;
    }


    
    public List<Carta> embaralharLista(List<String> lista, CorCarta timeInicio, CorCarta outroTime){

        int linhasTotais = lista.size();
        int fimLinhas = linhasTotais - 25; //pela randomização utilizada, será uma contagem decrescente do fim dela, até (ela - 25)

        List<String> palavrasSorteadas = new ArrayList<String>();
        Random random = new Random();

        for(int i = linhasTotais - 1; i >= fimLinhas; i--){
            int j = random.nextInt(i + 1);

            String temp = lista.get(i);
            lista.set(i, lista.get(j));
            lista.set(j, temp);

            palavrasSorteadas.add(lista.get(i)); 
        }

        List<CorCarta> cores = new ArrayList<>();
        for (int i = 0; i < 9; i++) cores.add(timeInicio);      // 9 cartas do primeiro time
        for (int i = 0; i < 8; i++) cores.add(outroTime);        // 8 cartas do segundo time
        for (int i = 0; i < 7; i++) cores.add(CorCarta.NEUTRA);  // 7 neutras
        for (int i = 0; i < 1; i++) cores.add(CorCarta.ASSASSINO); // 1 assassino
        Collections.shuffle(cores, random);  // Embaralha a ordem das cores 

        List<Carta> listaCartas = new ArrayList<Carta>();
        for (int i = 0; i < 25; i++) {
            listaCartas.add(new Carta(palavrasSorteadas.get(i), cores.get(i), i+1));
        }

        return listaCartas; 
    }

    public List<Carta> gerarTabuleiro(CorCarta timeInicio, CorCarta outroTime){
        InputStream pacotePalavras = GeradorTabuleiro.class.getResourceAsStream("/resources/palavras.txt");
        Scanner lista = new Scanner(pacotePalavras, "UTF-8");
        List<String> listaProcessada = scanearPacote(lista);
        List<Carta> tabuleiro = embaralharLista(listaProcessada, timeInicio, outroTime);
    
        return tabuleiro;
    }
}

