package com.viniciusdev.proeditor.utils;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import java.util.List;

public class DiffUtil {

    /**
     * Gera uma diferença entre o texto original e o novo texto.
     *
     * @param originalText o texto original
     * @param newText      o novo texto
     * @return uma string formatada representando as diferenças
     */
    public static String getDiff(String originalText, String newText) {
        // Divide os textos originais e novos em linhas
        List<String> originalLines = List.of(originalText.split("\n"));
        List<String> newLines = List.of(newText.split("\n"));

        // Gera o patch de diferença
        Patch<String> patch = DiffUtils.diff(originalLines, newLines);

        // Constrói a string de diferença formatada
        StringBuilder prettyDiff = new StringBuilder();
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            switch (delta.getType()) {
                case INSERT:
                    appendLines(prettyDiff, "[ADICIONADO]", delta.getTarget().getLines());
                    break;
                case DELETE:
                    appendLines(prettyDiff, "[REMOVIDO]", delta.getSource().getLines());
                    break;
                case CHANGE:
                    prettyDiff.append("[ALTERADO] DE ");
                    appendLines(prettyDiff, delta.getSource().getLines());
                    prettyDiff.append(" PARA ");
                    appendLines(prettyDiff, delta.getTarget().getLines());
                    break;
            }
        }

        return prettyDiff.toString();
    }

    /**
     * Adiciona uma lista de linhas à string de diferença com um prefixo.
     *
     * @param diff   o StringBuilder ao qual adicionar
     * @param prefix o prefixo a ser adicionado antes de cada linha
     * @param lines  a lista de linhas a serem adicionadas
     */
    private static void appendLines(StringBuilder diff, String prefix, List<String> lines) {
        for (String line : lines) {
            diff.append(prefix).append(" ").append(line).append("\n");
        }
    }

    /**
     * Adiciona uma lista de linhas à string de diferença sem um prefixo.
     *
     * @param diff  o StringBuilder ao qual adicionar
     * @param lines a lista de linhas a serem adicionadas
     */
    private static void appendLines(StringBuilder diff, List<String> lines) {
        for (String line : lines) {
            diff.append(line).append("\n");
        }
    }
}